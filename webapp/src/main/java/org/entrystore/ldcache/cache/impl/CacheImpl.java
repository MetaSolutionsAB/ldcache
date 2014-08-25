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
import org.entrystore.ldcache.util.HttpUtil;
import org.entrystore.ldcache.util.JsonUtil;
import org.entrystore.ldcache.util.ModelUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
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

	int cachingRetriesOnError = 0;

	long cachingTimeBetweenRetries = 1000;

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

		if (cacheConfig.has("retriesOnError")) {
			cachingRetriesOnError = cacheConfig.getInt("retriesOnError");
			log.info("Setting retries on error to " + cachingRetriesOnError);
		}

		if (cacheConfig.has("timeBetweenRetries")) {
			cachingTimeBetweenRetries = cacheConfig.getLong("timeBetweenRetries");
			log.info("Setting time between retries to " + cachingTimeBetweenRetries + " ms");
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
						populateDatabundle(databundles.getJSONObject(idx));
					} catch (JSONException e) {
						log.error(e.getMessage());
					}
				}
			});
		}
	}

	private void populateDatabundle(JSONObject databundle) throws JSONException {
		String name = "no name found";
		if (databundle.has("name")) {
			name = databundle.getString("name");
		}
		log.info("Populating databundle \"" + name + "\"");
		Date begin = new Date();

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

		org.json.JSONArray includeLiteralLanguages = null;
		if (databundle.has("includeLiteralLanguages")) {
			includeLiteralLanguages = databundle.getJSONArray("includeLiteralLanguages");
		}

		int followDepth = 2;
		if (databundle.has("followDepth")) {
			followDepth = databundle.getInt("followDepth");
		}

		loadAndCacheResources(JsonUtil.jsonArrayToURISet(resources), JsonUtil.jsonArrayToURISet(follow), JsonUtil.jsonArrayToMap(followTuples), JsonUtil.jsonArrayToStringSet(includeDestinations), JsonUtil.jsonArrayToStringSet(includeLiteralLanguages, true), followDepth);

		long duration = (new Date().getTime() - begin.getTime())/1000;
		log.info("Finished populating databundle \"" + name + "\" in " + duration + " seconds");
	}

	private Model loadResources(Set<URI> resources, Set<URI> propertiesToFollow, Map<URI, URI> followTuples, Set<String> includeDestinations, Set<String> includeLiteralLanguages, int level, int depth, boolean loadAndCache, boolean returnModel) {
		Model result = new LinkedHashModel();
		for (URI r : resources) {
			Model graph = null;
			if (RdfResource.hasResource(repository, (URI) r)) {
				graph = RdfResource.loadFromRepository(repository, (URI) r).getGraph();
				graph = ModelUtil.filterLanguageLiterals(graph, r, includeLiteralLanguages);
			} else {
				if (loadAndCache) {
					throttle((URI) r);
					graph = HttpUtil.getModelFromResponse(r, HttpUtil.getResourceFromURL(r.toString(), 0, cachingRetriesOnError, cachingTimeBetweenRetries));
					if (graph != null) {
						graph = ModelUtil.filterLanguageLiterals(graph, r, includeLiteralLanguages);
						RdfResource res = new RdfResource((URI) r, graph, new Date());
						RdfResource.saveToRepository(repository, res);
						log.info("Cached <" + r + ">");
					} else {
						log.warn("Model was null for <" + r + ">");
					}
				}
			}

			if (graph != null) {
				if (returnModel) {
					result.addAll(graph);
				}
				if (propertiesToFollow != null && level < depth) {
					Set<URI> objects = new HashSet<>();
					for (URI prop : propertiesToFollow) {
						objects.addAll(ModelUtil.valueToURI(graph.filter(null, (URI) prop, null).objects()));
					}
					if (followTuples != null) {
						objects.addAll(getMatchingSubjects(graph, followTuples));
					}
					objects = filterResources(objects, includeDestinations);
					objects.remove(r);
					if (objects.size() > 0) {
						log.debug("Crawling " + objects.size() + " resource" + (objects.size() == 1 ? "" : "s") + " linked from <" + r + ">: " + objects);
						result.addAll(loadResources(objects, propertiesToFollow, followTuples, includeDestinations, includeLiteralLanguages, level + 1, depth, loadAndCache, returnModel));
					}
				}
			}
		}
		return result;
	}

	@Override
	public void loadAndCacheResources(Set<URI> resources, Set<URI> follow, Map<URI, URI> followTuples, Set<String> includeDestinations, Set<String> includeLiteralLanguages, int depth) {
		loadResources(resources, follow, followTuples, includeDestinations, includeLiteralLanguages, 0, depth, true, false);
	}

	@Override
	public Model getMergedGraphs(Set<URI> resources, Set<URI> follow, Map<URI, URI> followTuples, Set<String> includeDestinations, Set<String> includeLiteralLanguages, int depth) {
		return loadResources(resources, follow, followTuples, includeDestinations, includeLiteralLanguages, 0, depth, false, true);
	}

	public Repository getRepository() {
		return this.repository;
	}

	private Set<URI> filterResources(Set<URI> resources, Set<String> allowedPrefixes) {
		if (resources == null || allowedPrefixes == null) {
			throw new IllegalArgumentException("Parameters must not be null");
		}
		if (allowedPrefixes.contains("*")) {
			return resources;
		}
		Set<URI> result = new HashSet<>();
		for (URI r : resources) {
			for (String p : allowedPrefixes) {
				if (r.stringValue().startsWith(p)) {
					result.add(r);
				}
			}
		}
		return result;
	}

	private Set<URI> getMatchingSubjects(Model model, Map<URI, URI> tuplesPO) {
		if (model == null || tuplesPO == null) {
			throw new IllegalArgumentException();
		}
		Set<URI> result = new HashSet<>();
		for (URI v : tuplesPO.keySet()) {
			result.addAll(ModelUtil.resourceToURI(model.filter(null, (URI) v, tuplesPO.get(v)).subjects()));
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