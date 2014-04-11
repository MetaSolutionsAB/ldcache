package org.entrystore.ldcache.resources;

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.resource.ServerResource;

import java.util.HashMap;

/**
 * @author Hannes Ebner
 */
public class BaseResource extends ServerResource {

	protected HashMap<String,String> parameters;

	private static Logger log = Logger.getLogger(BaseResource.class);

	@Override
	public void init(Context c, Request request, Response response) {
		parameters = parseRequest(request.getResourceRef().getRemainingPart());
		super.init(c, request, response);
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

}