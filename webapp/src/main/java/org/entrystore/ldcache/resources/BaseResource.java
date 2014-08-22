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
import org.entrystore.ldcache.LDCache;
import org.entrystore.ldcache.util.NS;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.resource.ServerResource;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Hannes Ebner
 */
public class BaseResource extends ServerResource {

	protected HashMap<String,String> parameters;

	private static Logger log = Logger.getLogger(BaseResource.class);

	URI url;

	int followDepth;

	Set<URI> follow;

	Map<URI, URI> followTuples;

	Set<String> includeDestinations;

	@Override
	public void init(Context c, Request request, Response response) {
		parameters = parseRequest(request.getResourceRef().getRemainingPart());
		prepareParameters();
		super.init(c, request, response);
	}

	@Override
	protected void doRelease() {
		url = null;
		followDepth = 0;
		follow = null;
		followTuples = null;
		includeDestinations = null;
	}

	static public HashMap<String, String> parseRequest(String request) {
		HashMap<String, String> argsAndVal = new HashMap<String, String>();

		int r = request.lastIndexOf("?");
		String req = request.substring(r + 1);
		String[] arguments = req.split("&");

		try {
			for (int i = 0; i < arguments.length; i++) {
				if (arguments[i].contains("=")) {
					String[] elements = arguments[i].split("=");
					argsAndVal.put(elements[0], elements[1]);
				} else {
					argsAndVal.put(arguments[i], "");
				}
			}
		} catch (IndexOutOfBoundsException e) {
			// special case!
			argsAndVal.put(req, "");
		}
		return argsAndVal;
	}

	private void prepareParameters() {
		if (parameters.containsKey("url")) {
			url = new URIImpl(urlDecode(parameters.get("url")));
		}

		if (parameters.containsKey("follow")) {
			String followStr = urlDecode(parameters.get("follow"));
			if (followStr != null) {
				follow = new HashSet();
				String[] followSplitStr = followStr.split(",");
				for (String s : followSplitStr) {
					follow.add(new URIImpl(NS.expandNS(s.trim())));
				}
			}
		}

		if (parameters.containsKey("followTuples")) {
			String followTupleStr = urlDecode(parameters.get("followTuples"));
			if (followTupleStr != null) {
				followTuples = new HashMap<>();
				String[] followTupleSplitStr = followTupleStr.split(",");
				for (String s : followTupleSplitStr) {
					String[] tuple = s.split("|");
					followTuples.put(new URIImpl(NS.expandNS(tuple[0])), new URIImpl(NS.expandNS(tuple[1])));
				}
			}
		}

		includeDestinations = new HashSet();
		if (parameters.containsKey("includeDestinations")) {
			String includeDestinationsStr = urlDecode(parameters.get("includeDestinations"));
			if (includeDestinationsStr != null) {
				String[] includeSplitStr = includeDestinationsStr.split(",");
				for (String s : includeSplitStr) {
					includeDestinations.add(NS.expandNS(s.trim()));
				}
			}
		}
		// default is "all destinations allowed"
		if (includeDestinations.size() == 0) {
			includeDestinations.add("*");
		}

		if (parameters.containsKey("followDepth")) {
			try {
				followDepth = Integer.valueOf(parameters.get("followDepth"));
			} catch (NumberFormatException nfe) {
				log.error(nfe.getMessage());
			}
		}
	}

	private String urlDecode(String input) {
		if (input != null) {
			try {
				return URLDecoder.decode(input, "UTF-8");
			} catch (UnsupportedEncodingException uee) {
				log.error(uee.getMessage());
			}
		}
		return null;
	}

	public LDCache getLDCache() {
		return ((LDCache) getContext().getAttributes().get(LDCache.KEY));
	}

}