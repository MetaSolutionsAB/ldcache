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
import org.entrystore.ldcache.LDCache;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Hannes Ebner
 */
public class HttpUtil {

	private static Logger log = Logger.getLogger(HttpUtil.class);

	private static Client client;

	private static String USERAGENT;

	static {
		Context clientContext = new Context();
		client = new Client(clientContext, Arrays.asList(Protocol.HTTP, Protocol.HTTPS));
		setTimeouts(10000);
		log.debug("Initialized HTTP client");
		USERAGENT = new StringBuffer().
				append("LDCache/").append(LDCache.getVersion().trim()).
				append(" (").
				append(System.getProperty("os.arch")).append("; ").
				append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("; ").
				append("Java; ").
				append(System.getProperty("java.vendor")).append(" ").append(System.getProperty("java.version")).
				append(")").
				toString();
		log.debug("User-Agent for HTTP requests set to \"" + USERAGENT + "\"");
	}

	public static Response getResourceFromURL(String url, int loopCount) {
		if (loopCount > 10) {
			log.warn("More than 10 redirect loops detected, aborting");
			return null;
		}

		Request request = new Request(Method.GET, url);
		request.getClientInfo().setAcceptedMediaTypes(RdfMedia.RDF_FORMATS);
		request.getClientInfo().setAgent(USERAGENT);
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
				log.debug("Request redirected from <" + url + "> to <" + refURL + ">");
				return getResourceFromURL(refURL, loopCount + 1);
			}
		}

		if (response.getEntity() != null && response.getEntity().getLocationRef() != null && response.getEntity().getLocationRef().getBaseRef() == null) {
			response.getEntity().getLocationRef().setBaseRef(url.substring(0, url.lastIndexOf("/")+1));
		}
		return response;
	}

	public static Model getModelFromResponse(URI r, Response response) {
		if (response == null) {
			throw new IllegalArgumentException();
		}
		if (response.getStatus().isError()) {
			log.warn("Skipping response from <" + r + "> due to error status: " + response.getStatus());
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

	public static HashMap<String, String> parseRequest(String request) {
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

	public static String readFirstLine(URL url) {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(url.openStream()));
			return in.readLine();
		} catch (IOException ioe) {
			log.error(ioe.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}
		}
		return null;
	}

}