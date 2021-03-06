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
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.restlet.data.MediaType;
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

		log.info("Received request for: " + url);

		Model resultGraph = cache.getMergedGraphs(new HashSet<URI>(Arrays.asList(url)), follow, followTuples, includeDestinations, includeLiteralLanguages, followDepth);
		if (resultGraph.size() > 0) {
			String outputMediaType = null;
			if (parameters.containsKey("format")) {
				outputMediaType = parameters.get("format");
			}
			if (outputMediaType == null) {
				outputMediaType = getRequest().getClientInfo().getPreferredMediaType(RdfMedia.SUPPORTED_MEDIA_TYPES).getName();
			}
			if (outputMediaType == null) {
				outputMediaType = "application/rdf+xml";
			}
			RDFFormat rdfFormat = RDFFormat.forMIMEType(outputMediaType);
			if (rdfFormat != null) {
				log.debug("Writing content in format " + outputMediaType);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					Rio.write(resultGraph, baos, rdfFormat);
				} catch (RDFHandlerException e) {
					log.error("RDF handler " + e.getMessage());
				}
				return new ByteArrayRepresentation(baos.toByteArray(), MediaType.valueOf(outputMediaType));
			} else {
				log.debug("Received request with MIME type that cannot be understood by Sesame Rio: " + outputMediaType);
				getResponse().setStatus(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);
				return null;
			}
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

		org.json.JSONObject followTuples = null;
		if (json.has("followTuples")) {
			try {
				followTuples = json.getJSONObject("followTuples");
			} catch (JSONException e) {
				log.error(e.getMessage());
			}
		}

		org.json.JSONArray includeDestinations = null;
		if (json.has("includeDestinations")) {
			try {
				includeDestinations = json.getJSONArray("includeDestinations");
			} catch (JSONException e) {
				log.error(e.getMessage());
			}
		}

		org.json.JSONArray includeLiteralLanguages = null;
		if (json.has("includeLiteralLanguages")) {
			try {
				includeLiteralLanguages = json.getJSONArray("includeLiteralLanguages");
			} catch (JSONException e) {
				log.error(e.getMessage());
			}
		}

		int depth = 2;
		if (json.has("depth")) {
			try {
				// FIXME should we accept only a certain max depth to not stretch the limits of
				// the server too much, e.g. maxdepth=2? This should probably be configurable.
				depth = json.getInt("depth");
			} catch (JSONException e) {
				log.error(e.getMessage());
			}
		}

		// FIXME start a thread to do the following

		cache.loadAndCacheResources(JsonUtil.jsonArrayToURISet(toAdd), JsonUtil.jsonArrayToURISet(toFollow), JsonUtil.jsonObjectToMap(followTuples), JsonUtil.jsonArrayToStringSet(includeDestinations), JsonUtil.jsonArrayToStringSet(includeLiteralLanguages), depth);

		getResponse().setStatus(Status.SUCCESS_OK); // FIXME change to ACCEPTED if run in background thread
	}

	private boolean hasAllParameters() {
		if (url != null) {
			return true;
		}
		return false;
	}

}