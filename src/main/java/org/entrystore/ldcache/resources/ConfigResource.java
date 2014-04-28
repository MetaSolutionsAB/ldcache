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

package org.entrystore.ldcache.resources;

import org.apache.log4j.Logger;
import org.entrystore.ldcache.cache.impl.RdfResource;
import org.entrystore.ldcache.util.HttpUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Hannes Ebner
 */
public class ConfigResource extends BaseResource {

	Logger log = Logger.getLogger(ConfigResource.class);

	Set<URI> visited = new HashSet<URI>();

	@Post("json")
	public void postJSON(Representation r) {
		JSONObject json = null;
		try {
			json = new JSONObject(r.getText());
		} catch (JSONException e) {
			log.error(e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		if (json == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;
		}

		org.json.JSONArray toAdd = null;
		try {
			toAdd = json.getJSONArray("add");
		} catch (JSONException e) {
			log.error(e.getMessage());
		}
		if (toAdd == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;
		}
		org.json.JSONArray toFollow = null;
		try {
			toFollow = json.getJSONArray("follow");
		} catch (JSONException e) {
			log.error(e.getMessage());
		}

		cacheResources(jsonArrayToSet(toAdd), jsonArrayToSet(toFollow), 0, 5); // FIXME start a thread to do this

		getResponse().setStatus(Status.SUCCESS_OK); // FIXME change to ACCEPTED if run in background thread
	}

	private Set<Value> jsonArrayToSet(JSONArray array) {
		Set<Value> result = new HashSet<Value>();
		for (int i = 0; i < array.length(); i++) {
			String uri = null;
			try {
				uri = array.getString(i);
			} catch (JSONException e) {
				log.warn(e.getMessage());
				continue;
			}
			if (uri != null) {
				result.add(new URIImpl(uri));
			}
		}
		return result;
	}

	private void cacheResources(Set<Value> resources, Set<Value> propertiesToFollow, int level, int depth) {
		for (Value r : resources) {
			if (!(r instanceof URI)) {
				continue;
			}
			if (visited.contains((URI) r)) {
				log.debug("Already visited, skipping: " + r);
				continue;
			}
			visited.add((URI) r);
			Model graph = HttpUtil.getModelFromRespone(HttpUtil.getResourceFromURL(r.toString(), 0));
			if (graph != null) {
				RdfResource res = new RdfResource((URI) r, graph, new Date());
				RdfResource.saveToRepository(getLDCache().getRepository(), res);
				log.info("Cached in local repository: " + r);
				if (propertiesToFollow != null && level < depth+1) {
					for (Value prop : propertiesToFollow) {
						if (prop instanceof URI) {
							log.info("Following: " + prop);
							Model matches = graph.filter((Resource) null, (URI) prop, (Value) null);
							cacheResources(matches.objects(), propertiesToFollow, ++level, depth);
						}
					}
				}
			} else {
				log.error("Model was null for: " + r.toString());
			}
		}
	}

}