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

package org.entrystore.ldcache;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.entrystore.ldcache.resources.CacheResource;
import org.entrystore.ldcache.resources.StatusResource;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

/**
 * EntryStore tools to manipulate Sesame repositories.
 *
 * @author Hannes Ebner
 */
public class LDCache extends Application {

	static Logger log = Logger.getLogger(LDCache.class);

	private static boolean verbose = false;

	public LDCache(Context parentContext) {
		super(parentContext);
	}

	@Override
	public synchronized Restlet createInboundRoot() {
		Router router = new Router(getContext());
		//router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);

		// global scope
		router.attach("/status", StatusResource.class);
		router.attach("/", CacheResource.class);

		// CORSFilter corsFilter = new CORSFilter();

		return router;
	}

	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		Component component = new Component();
		component.getServers().add(Protocol.HTTP, 8282);
		component.getClients().add(Protocol.FILE);
		component.getClients().add(Protocol.HTTP);
		component.getClients().add(Protocol.HTTPS);
		Context c = component.getContext().createChildContext();
		LDCache ldc = new LDCache(c);
		component.getDefaultHost().attach(ldc);

		try {
			component.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}