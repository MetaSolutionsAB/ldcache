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
import org.entrystore.ldcache.util.NS;
import org.openrdf.model.Graph;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Uniform;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.ByteArrayRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Hannes Ebner
 */
public class CacheResource extends BaseResource {

	static Logger log = Logger.getLogger(CacheResource.class);

	private Client client;

	private Response clientResponse;

	private Representation input;

	String url;

	int depth;

	Set<URI> follow;

	List<Preference<MediaType>> rdfFormats = new ArrayList<Preference<MediaType>>();

	List<MediaType> supportedMediaTypes = new ArrayList<MediaType>();

	private void prepareProxyRequest() {
		if (parameters.containsKey("url")) {
			url = urlDecode(parameters.get("url"));
		}

		if (parameters.containsKey("follow")) {
			String followStr = urlDecode(parameters.get("follow"));
			if (followStr != null) {
				follow = new HashSet();
				String[] followSplitStr = followStr.split(",");
				for (String s : followSplitStr) {
					s = NS.expandNS(s.trim());
					if (!s.contains(":")) {
						follow.add(new URIImpl(s));
					}
				}
			}
		}

		if (parameters.containsKey("depth")) {
			try {
				depth = Integer.valueOf(parameters.get("depth"));
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

	private boolean hasAllParameters() {
		if (url != null) {
			return true;
		}
		return false;
	}

	@Override
	protected void doInit() {
		rdfFormats.add(new Preference(MediaType.APPLICATION_RDF_XML));
		rdfFormats.add(new Preference(MediaType.APPLICATION_JSON));
		rdfFormats.add(new Preference(MediaType.TEXT_RDF_N3));
		rdfFormats.add(new Preference(new MediaType(RDFFormat.TURTLE.getDefaultMIMEType())));
		rdfFormats.add(new Preference(new MediaType(RDFFormat.TRIX.getDefaultMIMEType())));
		rdfFormats.add(new Preference(new MediaType(RDFFormat.NTRIPLES.getDefaultMIMEType())));
		rdfFormats.add(new Preference(new MediaType(RDFFormat.TRIG.getDefaultMIMEType())));
		rdfFormats.add(new Preference(new MediaType(RDFFormat.JSONLD.getDefaultMIMEType())));
		rdfFormats.add(new Preference(new MediaType(RDFFormat.RDFJSON.getDefaultMIMEType())));
		for (Preference<MediaType> p : rdfFormats) {
			supportedMediaTypes.add(p.getMetadata());
		}
		prepareProxyRequest();
	}

	@Override
	protected void doRelease() {
		url = null;
		depth = 0;
		follow = null;
		input = null;
	}

	@Get
	public Representation represent() {
		if (!hasAllParameters()) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return null;
		}

		log.info("Received caching request for " + url);

		clientResponse = getResourceFromURL(url, 0);
		input = null;
		MediaType representationMediaType = null;
		if (clientResponse != null) {
			input = clientResponse.getEntity();
			representationMediaType = input.getMediaType();
			log.debug("Received proxied resource in format " + representationMediaType);
			getResponse().setStatus(clientResponse.getStatus());
			getResponse().setOnSent(new Uniform() {
				public void handle(Request request, Response response) {
					try {
						clientResponse.release();
						clientResponse = null;
						client.stop();
						client = null;
					} catch (Exception e) {
						log.error("Error when releasing and stopping client: " + e.getMessage());
					}
				}
			});
		}

		Representation output = null;

		if (input != null && input.isAvailable() && representationMediaType != null) {
			InputStream content = null;
			try {
				content = input.getStream();
			} catch (IOException e) {
				log.error(e.getMessage());
			}
			if (content != null) {
				Graph deserializedGraph = null;
				log.debug("Requesting parser format for " + representationMediaType);
				RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(representationMediaType.getName());
				log.debug("Got parser format " + rdfFormat);
				if (rdfFormat != null) {
					try {
						deserializedGraph = Rio.parse(content, "", rdfFormat);
					} catch (IOException e) {
						log.error("IO error " + e.getMessage());
					} catch (RDFParseException e) {
						log.error("Unable to parse RDF " + e.getMessage());
					}

					if (deserializedGraph != null) {
						String outputMediaType = getRequest().getClientInfo().getPreferredMediaType(supportedMediaTypes).getName();
						log.debug("Writing content in format " + outputMediaType);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						try {
							Rio.write(deserializedGraph, baos, rdfFormat);
						} catch (RDFHandlerException e) {
							log.error("RDF handler " + e.getMessage());
						}
						output = new ByteArrayRepresentation(baos.toByteArray());
					}
				}
			}
		}

		if (output != null) {
			return output;
		}

		getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
		return null;
	}

	private Response getResourceFromURL(String url, int loopCount) {
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
		getRequest().getClientInfo().getAcceptedMediaTypes().addAll(rdfFormats);
		request.getClientInfo().setAcceptedMediaTypes(getRequest().getClientInfo().getAcceptedMediaTypes());
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

}