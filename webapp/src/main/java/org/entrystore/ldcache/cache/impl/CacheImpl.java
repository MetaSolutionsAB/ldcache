/*
 * Copyright (c) 2014 MetaSolutions AB <info@metasolutions.se>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.entrystore.ldcache.cache.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.log4j.Logger;
import org.entrystore.ldcache.cache.Cache;
import org.entrystore.ldcache.cache.Resource;
import org.entrystore.ldcache.util.HttpUtil;
import org.entrystore.ldcache.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.nativerdf.NativeStore;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Hannes Ebner
 */
public class CacheImpl implements Cache {

	static Logger log = Logger.getLogger(CacheImpl.class);

	Repository repository;

	JSONObject config;

	com.google.common.cache.Cache<String, RateLimiter> rateLimiters;

	double rateLimit = 10.0;

	public CacheImpl(JSONObject config) throws JSONException {
		this.config = config;

		JSONObject repoConfig = config.getJSONObject("repository");
		String repositoryType = repoConfig.getString("type");
		Sail sail = null;
		if ("memory".equalsIgnoreCase(repositoryType)) {
			sail = new MemoryStore();
		} else if ("native".equalsIgnoreCase(repositoryType)) {
			sail = new NativeStore(new File(java.net.URI.create(repoConfig.getString("uri"))));
		} else {
			throw new IllegalArgumentException("Invalid repository type");
		}

		try {
			this.repository = new SailRepository(sail);
			repository.initialize();
		} catch (RepositoryException e) {
			log.error(e.getMessage());
		}

		JSONObject cacheConfig = config.getJSONObject("cache");
		if (cacheConfig.has("rateLimit")) {
			rateLimit = cacheConfig.getDouble("rateLimit");
		}
		rateLimiters = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).maximumSize(1000).build();

		populateDatasets(config.getJSONArray("datasets"));
	}

	private void populateDatasets(JSONArray datasets) throws JSONException {
		for (int i = 0; i < datasets.length(); i++) {
			populateResources(datasets.getJSONObject(i));
		}
	}

	private void populateResources(JSONObject dataset) throws JSONException {
		org.json.JSONArray resources = null;
		if (dataset.has("resources")) {
			resources = dataset.getJSONArray("resources");
		}

		org.json.JSONArray follow = null;
		if (dataset.has("follow")) {
			follow = dataset.getJSONArray("follow");
		}

		org.json.JSONArray followTuples = null;
		if (dataset.has("followTuples")) {
			followTuples = dataset.getJSONArray("followTuples");
		}

		org.json.JSONArray includeDestinations = null;
		if (dataset.has("includeDestinations")) {
			includeDestinations = dataset.getJSONArray("includeDestinations");
		}

		int followDepth = 2;
		if (dataset.has("followDepth")) {
			followDepth = dataset.getInt("followDepth");
		}

		// TODO start a thread to do the following; thread pool with configurable amount of threads

		loadAndCacheResources(JsonUtil.jsonArrayToSet(resources), JsonUtil.jsonArrayToSet(follow), JsonUtil.jsonArrayToMap(followTuples), JsonUtil.jsonArrayToSet(includeDestinations), followDepth);
	}

	private void loadAndCacheResources(Set<Value> resources, Set<Value> propertiesToFollow, Map<Value, Value> followTuples, Set<Value> includeDestinations, Set<URI> visited, int level, int depth) {

		// TODO add smartness to follow rdf:type by fetching the subject instead of the object

		for (Value r : resources) {
			if (!(r instanceof URI)) {
				continue;
			}
			if (visited.contains(r)) {
				log.debug("Already visited, skipping: " + r);
				continue;
			}

			throttle((URI) r);
			Model graph = HttpUtil.getModelFromResponse(HttpUtil.getResourceFromURL(r.toString(), 0));
			if (graph != null) {
				RdfResource res = new RdfResource((URI) r, graph, new Date());
				RdfResource.saveToRepository(this.repository, res);
				log.info("Cached in local repository: " + r);
				if (propertiesToFollow != null && level < depth+1) {
					for (Value prop : propertiesToFollow) {
						if (prop instanceof URI) {
							Set<Value> objects = graph.filter(null, (URI) prop, null).objects();
							objects = filterResources(objects, includeDestinations);
							if (objects.size() == 0) {
								continue;
							}
							log.debug("Following: " + prop);
							loadAndCacheResources(objects, propertiesToFollow, followTuples, includeDestinations, visited, ++level, depth);
						}
					}
				}
			} else {
				log.error("Model was null for: " + r.toString());
			}
			visited.add((URI) r);
		}
	}

	private Model getMergedGraphs(Set<Value> resources, Set<Value> follow, Map<Value, Value> followTuples, Set<Value> includeDestinations, Set<URI> visited, int level, int depth) {

		// TODO add smartness to follow rdf:type by fetching the subject instead of the object

		Model result = new LinkedHashModel();
		for (Value r : resources) {
			if (!(r instanceof URI)) {
				continue;
			}
			if (visited.contains(r)) {
				log.debug("Already visited, skipping: " + r);
				continue;
			}
			visited.add((URI) r);
			Resource res = RdfResource.loadFromRepository(this.repository, (URI) r);
			if (res == null || res.getGraph() == null) {
				continue;
			}
			log.info("Loaded from local repository: " + r);
			Model graph = res.getGraph();
			result.addAll(graph);
			if (follow != null && level < depth+1) {
				for (Value prop : follow) {
					if (prop instanceof URI) {
						Set<Value> objects = graph.filter(null, (URI) prop, null).objects();
						objects = filterResources(objects, includeDestinations);
						if (objects.size() == 0) {
							continue;
						}
						log.debug("Following: " + prop);
						result.addAll(getMergedGraphs(objects, follow, followTuples, includeDestinations, visited, ++level, depth));
					}
				}
			}
		}
		return result;
	}

	@Override
	public void loadAndCacheResources(Set<Value> resources, Set<Value> follow, Map<Value, Value> followTuples, Set<Value> includeDestination, int depth) {
		loadAndCacheResources(resources, follow, followTuples, includeDestination, new HashSet<URI>(), 0, depth);
	}

	@Override
	public Model getMergedGraphs(Set<Value> resources, Set<Value> follow, Map<Value, Value> followTuples, Set<Value> includeDestination, int depth) {
		return getMergedGraphs(resources, follow, followTuples, includeDestination, new HashSet<URI>(), 0, depth);
	}

	public Repository getRepository() {
		return this.repository;
	}

	private Set<Value> filterResources(Set<Value> resources, Set<Value> allowedPrefixes) {
		if (allowedPrefixes.contains("*")) {
			return resources;
		}
		Set<Value> result = new HashSet<Value>();
		for (Value v : resources) {
			for (Value p : allowedPrefixes) {
				if (v.stringValue().startsWith(p.stringValue())) {
					result.add(v);
				}
			}
		}
		return result;
	}


	void throttle(URI uri) {
		String hostname = java.net.URI.create(uri.stringValue()).getHost();
		try {
			rateLimiters.get(hostname, new Callable<RateLimiter>() {
				@Override
				public RateLimiter call() throws Exception {
					return RateLimiter.create(rateLimit);
				}
			}).acquire();
		} catch (ExecutionException e) {
			log.error(e.getMessage());
		}
	}

}