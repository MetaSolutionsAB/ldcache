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

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * @author Hannes Ebner
 */
public class StatusResource extends BaseResource {

	@Get("json")
	public Representation getJSON() throws JSONException {
		JSONObject result = new JSONObject();
		result.put("service", "ld-cache");
		return new JsonRepresentation(result);
	}

	@Get("html|xhtml|xml")
	public Representation getXHTML() {
		return new StringRepresentation("<html><body>ld-cache running</body></html>", MediaType.TEXT_HTML);
	}

}