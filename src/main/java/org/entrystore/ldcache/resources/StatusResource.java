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