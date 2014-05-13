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
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.restlet.data.MediaType;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Hannes Ebner
 */
public class StatusResource extends BaseResource {

	private static Logger log = Logger.getLogger(StatusResource.class);

	@Get("json")
	public Representation getJSON() throws JSONException {
		JSONObject result = new JSONObject();
		result.put("service", "ld-cache");
		return new JsonRepresentation(result);
	}

	@Get("html|xhtml|xml")
	public Representation getXHTML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<html>\n<head>\n<title>Linked Data Cache Status</title>\n</head>\n<body>\n");
		sb.append("<h3>Linked Data Cache Status</h3>\n");
		Set<Resource> cached = getCachedResources();
		sb.append("Number of resources cached: ").append(cached.size()).append("<br/>\n");
		sb.append("<pre>\n");
		for (Resource r : cached) {
			sb.append("<a href=\"").append(r).append("\">").append(r).append("</a>\n");
		}
		sb.append("</pre>\n");
		sb.append("</body>\n</html>");
		return new StringRepresentation(sb.toString(), MediaType.TEXT_HTML);
	}

	private Set<Resource> getCachedResources() {
		Set<Resource> result = new HashSet<Resource>();
		Repository repo = getLDCache().getCache().getRepository();
		RepositoryConnection rc = null;
		try {
			rc = repo.getConnection();
			RepositoryResult<Resource> rr = rc.getContextIDs();
			while (rr.hasNext()) {
				result.add(rr.next());
			}
		} catch (RepositoryException re) {
			log.error(re.getMessage());
		} finally {
			if (rc != null) {
				try {
					rc.close();
				} catch (RepositoryException e) {
					log.error(e.getMessage());
				}
			}
		}
		return result;
	}

}