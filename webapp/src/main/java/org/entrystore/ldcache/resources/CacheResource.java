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
import org.entrystore.ldcache.cache.Cache;
import org.entrystore.ldcache.util.JsonUtil;
import org.entrystore.ldcache.util.RdfMedia;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.restlet.data.Status;
import org.restlet.representation.ByteArrayRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author Hannes Ebner
 */
public class CacheResource extends BaseResource {

	static Logger log = Logger.getLogger(CacheResource.class);

	private Cache cache;

	@Get
	public Representation represent() {
		if (!hasAllParameters()) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return null;
		}
		this.cache = getLDCache().getCache();

		log.info("Received caching request for " + url);

		Model resultGraph = cache.getMergedGraphs(new HashSet<Value>(Arrays.asList(url)), follow, new HashSet<URI>(), 0, depth);
		if (resultGraph.size() > 0) {
			String outputMediaType = getRequest().getClientInfo().getPreferredMediaType(RdfMedia.SUPPORTED_MEDIA_TYPES).getName();
			log.debug("Writing content in format " + outputMediaType);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				Rio.write(resultGraph, baos, RDFFormat.forMIMEType(outputMediaType));
			} catch (RDFHandlerException e) {
				log.error("RDF handler " + e.getMessage());
			}
			return new ByteArrayRepresentation(baos.toByteArray());
		}

		getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		return null;
	}

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
		if (json.has("add")) {
			try {
				toAdd = json.getJSONArray("add");
			} catch (JSONException e) {
				log.error(e.getMessage());
			}
		}
		if (toAdd == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;
		}

		org.json.JSONArray toFollow = null;
		if (json.has("follow")) {
			try {
				toFollow = json.getJSONArray("follow");
			} catch (JSONException e) {
				log.error(e.getMessage());
			}
		}

		int depth = 2;
		if (json.has("depth")) {
			try {
				depth = json.getInt("depth");
			} catch (JSONException e) {
				log.error(e.getMessage());
			}
		}

		// FIXME start a thread to do the following

		cache.loadAndCacheResources(JsonUtil.jsonArrayToSet(toAdd), JsonUtil.jsonArrayToSet(toFollow), new HashSet<URI>(), 0, depth);

		getResponse().setStatus(Status.SUCCESS_OK); // FIXME change to ACCEPTED if run in background thread
	}

	private boolean hasAllParameters() {
		if (url != null) {
			return true;
		}
		return false;
	}

}