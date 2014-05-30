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
import org.entrystore.ldcache.util.HttpUtil;
import org.entrystore.ldcache.util.RdfMedia;
import org.openrdf.model.Graph;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Uniform;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.ByteArrayRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Hannes Ebner
 */
public class ProxyResource extends BaseResource {

	static Logger log = Logger.getLogger(ProxyResource.class);

	private Response clientResponse;

	private Representation input;

	private boolean hasAllParameters() {
		if (url != null) {
			return true;
		}
		return false;
	}

	@Get
	public Representation represent() {

		// FIXME only allow requests to URLs which are defined in configuration

		if (!hasAllParameters()) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return null;
		}

		log.info("Received proxy request for " + url);

		clientResponse = HttpUtil.getResourceFromURL(url.toString(), 0);
		input = null;
		MediaType representationMediaType = null;
		if (clientResponse != null) {
			input = clientResponse.getEntity();
			representationMediaType = input.getMediaType();
			log.debug("Received proxied resource in format " + representationMediaType);
			getResponse().setStatus(clientResponse.getStatus());
			getResponse().setOnSent(new Uniform() {
				public void handle(Request request, Response response) {
					clientResponse.release();
					clientResponse = null;
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
						log.debug("Writing content in format " + outputMediaType);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						try {
							Rio.write(deserializedGraph, baos, RDFFormat.forMIMEType(outputMediaType));
						} catch (RDFHandlerException e) {
							log.error(e.getMessage());
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

}