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

package org.entrystore.ldcache.util;

import org.apache.log4j.Logger;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Hannes Ebner
 */
public class HttpUtil {

	private static Logger log = Logger.getLogger(HttpUtil.class);

	private static Client client;

	public static Response getResourceFromURL(String url, int loopCount) {
		if (loopCount > 10) {
			log.warn("More than 10 redirect loops detected, aborting");
			return null;
		}

		if (client == null) {
			client = new Client(Protocol.HTTP);
			client.setContext(new Context());
			client.getContext().getParameters().add("connectTimeout", "15000");
			client.getContext().getParameters().add("readTimeout", "15000");
			log.debug("Initialized HTTP client");
		}

		Request request = new Request(Method.GET, url);
		request.getClientInfo().setAcceptedMediaTypes(RdfMedia.RDF_FORMATS);
		Response response = client.handle(request);

		if (response.getStatus().isRedirection()) {
			Reference ref = response.getLocationRef();
			response.getEntity().release();
			if (ref != null) {
				String refURL = ref.getIdentifier();
				log.info("Request redirected to " + refURL);
				return getResourceFromURL(refURL, ++loopCount);
			}
		}

		if (response.getEntity().getLocationRef() != null && response.getEntity().getLocationRef().getBaseRef() == null) {
			response.getEntity().getLocationRef().setBaseRef(url.substring(0, url.lastIndexOf("/")+1));
		}
		return response;
	}

	public static Model getModelFromResponse(Response response) {
		if (response == null) {
			throw new IllegalArgumentException();
		}
		Model result = null;
		Representation input = response.getEntity();
		MediaType mt = input.getMediaType();

		if (input != null && input.isAvailable() && !input.isEmpty() && mt != null) {
			InputStream content = null;
			try {
				content = input.getStream();
			} catch (IOException e) {
				log.error(e.getMessage());
			}
			if (content != null) {
				log.debug("Requesting parser format for " + mt);
				RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(mt.getName());
				log.debug("Got parser format " + rdfFormat);
				if (rdfFormat != null) {
					try {
						result = Rio.parse(content, "", rdfFormat);
					} catch (IOException e) {
						log.error("IO error " + e.getMessage());
					} catch (RDFParseException e) {
						log.error("Unable to parse RDF " + e.getMessage());
					}
				}
			}
		}
		return result;
	}

	public static void throttle(URI resource) {
		// TODO calls sleep with respect to amount of requests per server in a specific time period
	}

}