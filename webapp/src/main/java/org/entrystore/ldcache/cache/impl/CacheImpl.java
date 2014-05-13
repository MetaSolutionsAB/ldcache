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

import org.apache.log4j.Logger;
import org.entrystore.ldcache.cache.Cache;
import org.entrystore.ldcache.cache.Resource;
import org.entrystore.ldcache.util.HttpUtil;
import org.json.JSONObject;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import java.util.Date;
import java.util.Set;

/**
 * @author Hannes Ebner
 */
public class CacheImpl implements Cache {

	static Logger log = Logger.getLogger(CacheImpl.class);

	Repository repository;

	JSONObject config;

	public CacheImpl(JSONObject config) {
		this.config = config;
		this.repository = new SailRepository(new MemoryStore()); // FIXME
		try {
			repository.initialize();
		} catch (RepositoryException e) {
			log.error(e.getMessage());
		}
	}

	public void loadAndCacheResources(Set<Value> resources, Set<Value> propertiesToFollow, Set<URI> visited, int level, int depth) {

		// FIXME add smartness to follow rdf:type by fetching the subject instead of the object

		for (Value r : resources) {
			if (!(r instanceof URI)) {
				continue;
			}
			if (visited.contains(r)) {
				log.debug("Already visited, skipping: " + r);
				continue;
			}
			visited.add((URI) r);
			Model graph = HttpUtil.getModelFromResponse(HttpUtil.getResourceFromURL(r.toString(), 0));
			if (graph != null) {
				RdfResource res = new RdfResource((URI) r, graph, new Date());
				RdfResource.saveToRepository(this.repository, res);
				log.info("Cached in local repository: " + r);
				if (propertiesToFollow != null && level < depth+1) {
					for (Value prop : propertiesToFollow) {
						if (prop instanceof URI) {
							log.debug("Following: " + prop);
							Model matches = graph.filter(null, (URI) prop, null);
							loadAndCacheResources(matches.objects(), propertiesToFollow, visited, ++level, depth);
						}
					}
				}
			} else {
				log.error("Model was null for: " + r.toString());
			}
		}
	}

	public Model getMergedGraphs(Set<Value> resources, Set<Value> propertiesToFollow, Set<URI> visited, int level, int depth) {
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
			if (propertiesToFollow != null && level < depth+1) {
				for (Value prop : propertiesToFollow) {
					if (prop instanceof URI) {
						log.debug("Following: " + prop);
						Model matches = graph.filter(null, (URI) prop, null);
						result.addAll(getMergedGraphs(matches.objects(), propertiesToFollow, visited, ++level, depth));
					}
				}
			}
		}
		return result;
	}

}