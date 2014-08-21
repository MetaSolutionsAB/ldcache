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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Hannes Ebner
 */
public class CacheImpl implements Cache {

	static Logger log = Logger.getLogger(CacheImpl.class);

	Repository repository;

	JSONObject config;

	com.google.common.cache.Cache<String, RateLimiter> rateLimiters;

	double rateLimit = 2.0;

	ExecutorService executor;

	public CacheImpl(JSONObject config) throws JSONException {
		this.config = config;

		JSONObject repoConfig = config.getJSONObject("repository");
		String repositoryType = repoConfig.getString("type");
		String indexes = repoConfig.getString("indexes");
		Sail sail = null;
		if ("memory".equalsIgnoreCase(repositoryType)) {
			sail = new MemoryStore();
		} else if ("native".equalsIgnoreCase(repositoryType)) {
			sail = new NativeStore(new File(java.net.URI.create(repoConfig.getString("uri"))));
			if (indexes != null && indexes.trim().length() > 3) {
				((NativeStore) sail).setTripleIndexes(indexes);
			}
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

		int threadPoolSize = 5;
		if (cacheConfig.has("threadPoolSize")) {
			threadPoolSize = cacheConfig.getInt("threadPoolSize");
		}
		log.info("Creating fixed thread pool with size " + threadPoolSize);
		executor = Executors.newFixedThreadPool(threadPoolSize);

		if (cacheConfig.has("requestTimeout")) {
			long timeout = cacheConfig.getLong("requestTimeout");
			log.info("Setting request timeout to " + timeout);
			HttpUtil.setTimeouts(timeout);
		}

		populateDatabundles(config.getJSONArray("databundles"));
	}

	private void populateDatabundles(final JSONArray databundles) throws JSONException {
		for (int i = 0; i < databundles.length(); i++) {
			final int idx = i;
			executor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						populateResources(databundles.getJSONObject(idx));
					} catch (JSONException e) {
						log.error(e.getMessage());
					}
				}
			});
		}
	}

	private void populateResources(JSONObject databundle) throws JSONException {
		String name = "no name found";
		if (databundle.has("name")) {
			name = databundle.getString("name");
		}
		log.info("Populating databundle: " + name);

		org.json.JSONArray resources = null;
		if (databundle.has("resources")) {
			resources = databundle.getJSONArray("resources");
		}

		org.json.JSONArray follow = null;
		if (databundle.has("follow")) {
			follow = databundle.getJSONArray("follow");
		}

		org.json.JSONArray followTuples = null;
		if (databundle.has("followTuples")) {
			followTuples = databundle.getJSONArray("followTuples");
		}

		org.json.JSONArray includeDestinations = null;
		if (databundle.has("includeDestinations")) {
			includeDestinations = databundle.getJSONArray("includeDestinations");
		}

		int followDepth = 2;
		if (databundle.has("followDepth")) {
			followDepth = databundle.getInt("followDepth");
		}

		loadAndCacheResources(JsonUtil.jsonArrayToValueSet(resources), JsonUtil.jsonArrayToValueSet(follow), JsonUtil.jsonArrayToMap(followTuples), JsonUtil.jsonArrayToStringSet(includeDestinations), followDepth);
	}

	private void loadAndCacheResources(Set<Value> resources, Set<Value> propertiesToFollow, Map<Value, Value> followTuples, Set<String> includeDestinations, Set<URI> visited, int level, int depth) {
		for (Value r : resources) {
			if (!(r instanceof URI)) {
				continue;
			}
			if (visited.contains(r)) {
				log.debug("Already visited, skipping: " + r);
				continue;
			}

			Model graph = null;
			if (RdfResource.hasResource(repository, (URI) r)) {
				graph = RdfResource.loadFromRepository(repository, (URI) r).getGraph();
			} else {
				throttle((URI) r);
				graph = HttpUtil.getModelFromResponse(HttpUtil.getResourceFromURL(r.toString(), 0));
				if (graph != null) {
					RdfResource res = new RdfResource((URI) r, graph, new Date());
					RdfResource.saveToRepository(repository, res);
					log.info("Cached: " + r);
				} else {
					log.warn("Model was null for: " + r.toString());
				}
			}

			if (propertiesToFollow != null && level < depth) {
				for (Value prop : propertiesToFollow) {
					if (prop instanceof URI) {
						Set<Value> objects = new HashSet<>(graph.filter(null, (URI) prop, null).objects());
						if (followTuples != null) {
							objects.addAll(getMatchingSubjects(graph, followTuples));
						}
						objects = filterResources(objects, includeDestinations);
						if (objects.size() == 0) {
							continue;
						}
						log.debug("Following: " + prop);
						loadAndCacheResources(objects, propertiesToFollow, followTuples, includeDestinations, visited, level + 1, depth);
					}
				}
			}

			visited.add((URI) r);
		}
	}

	private Model getMergedGraphs(Set<Value> resources, Set<Value> follow, Map<Value, Value> followTuples, Set<String> includeDestinations, Set<URI> visited, int level, int depth) {
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
						Set<Value> objects = new HashSet<>(graph.filter(null, (URI) prop, null).objects());
						if (followTuples != null) {
							objects.addAll(getMatchingSubjects(graph, followTuples));
						}
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
	public void loadAndCacheResources(Set<Value> resources, Set<Value> follow, Map<Value, Value> followTuples, Set<String> includeDestinations, int depth) {
		loadAndCacheResources(resources, follow, followTuples, includeDestinations, new HashSet<URI>(), 0, depth);
	}

	@Override
	public Model getMergedGraphs(Set<Value> resources, Set<Value> follow, Map<Value, Value> followTuples, Set<String> includeDestinations, int depth) {
		return getMergedGraphs(resources, follow, followTuples, includeDestinations, new HashSet<URI>(), 0, depth);
	}

	public Repository getRepository() {
		return this.repository;
	}

	private Set<Value> filterResources(Set<Value> resources, Set<String> allowedPrefixes) {
		if (resources == null || allowedPrefixes == null) {
			throw new IllegalArgumentException("Parameters must not be null");
		}
		if (allowedPrefixes.contains("*")) {
			return resources;
		}
		Set<Value> result = new HashSet<>();
		for (Value v : resources) {
			for (String p : allowedPrefixes) {
				if (v.stringValue().startsWith(p)) {
					result.add(v);
				}
			}
		}
		return result;
	}

	private Set<Value> getMatchingSubjects(Model model, Map<Value, Value> tuplesPO) {
		if (model == null || tuplesPO == null) {
			throw new IllegalArgumentException();
		}
		Set<Value> result = new HashSet<>();
		for (Value v : tuplesPO.keySet()) {
			result.addAll(model.filter(null, (URI) v, tuplesPO.get(v)).subjects());
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