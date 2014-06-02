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
import java.util.Arrays;

/**
 * @author Hannes Ebner
 */
public class HttpUtil {

	private static Logger log = Logger.getLogger(HttpUtil.class);

	private static Client client;

	static {
		Context clientContext = new Context();
		client = new Client(clientContext, Arrays.asList(Protocol.HTTP, Protocol.HTTPS));
		setTimeouts(10000);
		log.debug("Initialized HTTP client");
	}

	public static Response getResourceFromURL(String url, int loopCount) {
		if (loopCount > 10) {
			log.warn("More than 10 redirect loops detected, aborting");
			return null;
		}

		Request request = new Request(Method.GET, url);
		request.getClientInfo().setAcceptedMediaTypes(RdfMedia.RDF_FORMATS);
		Response response = client.handle(request);

		// Alternative to calling the client directly:
		// HttpClientHelper helper = new HttpClientHelper(client);
		// Response response = new Response(request);
		// helper.handle(request, response);

		if (response.getStatus().isRedirection()) {
			Reference ref = response.getLocationRef();
			try {
				response.getEntity().exhaust();
			} catch (IOException e) {
				log.warn(e.getMessage());
			}
			response.getEntity().release();
			if (ref != null) {
				String refURL = ref.getIdentifier();
				log.debug("Request redirected to " + refURL);
				return getResourceFromURL(refURL, loopCount + 1);
			}
		}

		if (response.getEntity() != null && response.getEntity().getLocationRef() != null && response.getEntity().getLocationRef().getBaseRef() == null) {
			response.getEntity().getLocationRef().setBaseRef(url.substring(0, url.lastIndexOf("/")+1));
		}
		return response;
	}

	public static Model getModelFromResponse(Response response) {
		if (response == null) {
			throw new IllegalArgumentException();
		}
		if (response.getStatus().isError()) {
			log.warn("Skipping response due to error status");
			return null;
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

	public static void setTimeouts(long timeout) {
		String timeoutStr = Long.toString(timeout);
		client.getContext().getParameters().set("connectTimeout", timeoutStr);
		client.getContext().getParameters().set("socketTimeout", timeoutStr);
		client.getContext().getParameters().set("readTimeout", timeoutStr);
		client.getContext().getParameters().set("socketConnectTimeoutMs", timeoutStr);
	}

}