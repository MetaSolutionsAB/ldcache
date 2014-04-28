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
import org.entrystore.ldcache.cache.Resource;
import org.entrystore.ldcache.cache.impl.RdfResource;
import org.entrystore.ldcache.util.RdfMedia;
import org.openrdf.repository.Repository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.restlet.Client;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.representation.ByteArrayRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import java.io.ByteArrayOutputStream;

/**
 * @author Hannes Ebner
 */
public class CacheResource extends BaseResource {

	static Logger log = Logger.getLogger(CacheResource.class);

	private Client client;

	private Response clientResponse;

	private Representation input;

	@Get
	public Representation represent() {
		if (!hasAllParameters()) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return null;
		}
		Representation output = null;
		log.info("Received caching request for " + url);

		Repository repository = getLDCache().getRepository();
		Resource resource = RdfResource.loadFromRepository(repository, url);

		if (resource != null && resource.getGraph() != null) {
			String outputMediaType = getRequest().getClientInfo().getPreferredMediaType(RdfMedia.SUPPORTED_MEDIA_TYPES).getName();
			log.debug("Writing content in format " + outputMediaType);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				Rio.write(resource.getGraph(), baos, RDFFormat.forMIMEType(outputMediaType));
			} catch (RDFHandlerException e) {
				log.error("RDF handler " + e.getMessage());
			}
			output = new ByteArrayRepresentation(baos.toByteArray());
		}

		if (output != null) {
			return output;
		}

		getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		return null;
	}

	private boolean hasAllParameters() {
		if (url != null) {
			return true;
		}
		return false;
	}

}