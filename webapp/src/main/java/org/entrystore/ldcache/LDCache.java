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
import org.entrystore.ldcache.cache.Cache;
import org.entrystore.ldcache.cache.impl.CacheImpl;
import org.entrystore.ldcache.filters.JSCallbackFilter;
import org.entrystore.ldcache.resources.CacheResource;
import org.entrystore.ldcache.resources.ProxyResource;
import org.entrystore.ldcache.resources.StatusResource;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * EntryStore tools to manipulate Sesame repositories.
 *
 * @author Hannes Ebner
 */
public class LDCache extends Application {

	static Logger log = Logger.getLogger(LDCache.class);

	private static boolean verbose = false;

	public static String KEY = "org.entryscape.ldcache.LDCache";

	private static String VERSION = null;

	Cache cache;

	JSONObject config;

	public LDCache(Context parentContext) throws IOException, JSONException {
		this(parentContext, null);
	}

	public LDCache(Context parentContext, URI configURI) throws IOException, JSONException {
		super(parentContext);
		getContext().getAttributes().put(KEY, this);
		if (configURI == null) {
			configURI = getConfigurationURI("ldcache.json");
		}

		if (configURI != null && "file".equals(configURI.getScheme())) {
			config = new JSONObject(new String(Files.readAllBytes(Paths.get(configURI))));
			configureLogging(config);
			cache = new CacheImpl(config);
		} else {
			log.error("No configuration found");
			System.exit(1);
		}
	}

	@Override
	public synchronized Restlet createInboundRoot() {
		Router router = new Router(getContext());
		router.setDefaultMatchingMode(Template.MODE_EQUALS);

		// global scope
		//router.attach("/config", ConfigResource.class); // TODO
		if (isProxyEnabled()) {
			router.attach("/proxy", ProxyResource.class);
			log.info("Proxy enabled");
		} else {
			log.info("Proxy disabled");
		}
		router.attach("/status", StatusResource.class);
		router.attach("/", CacheResource.class);

		JSCallbackFilter jsCallback = new JSCallbackFilter();
		jsCallback.setNext(router);

		// CORSFilter corsFilter = new CORSFilter();

		return jsCallback;
	}

	public Cache getCache() {
		return this.cache;
	}

	public static URI getConfigurationURI(String fileName) {
		URL resURL = Thread.currentThread().getContextClassLoader().getResource(fileName);
		try {
			if (resURL != null) {
				return resURL.toURI();
			}
		} catch (URISyntaxException e) {
			log.error(e.getMessage());
		}

		String classPath = System.getProperty("java.class.path");
		String[] pathElements = classPath.split(System.getProperty("path.separator"));
		for (String element : pathElements)	{
			File newFile = new File(element, fileName);
			if (newFile.exists()) {
				return newFile.toURI();
			}
		}
		log.error("Unable to find " + fileName + " in classpath");
		return null;
	}

	public static String getVersion() {
		if (VERSION == null) {
			URI versionFile = getConfigurationURI("VERSION.txt");
			try {
				VERSION = new String(Files.readAllBytes(Paths.get(versionFile)));
			} catch (IOException e) {
				VERSION = new SimpleDateFormat("yyyyMMdd").format(new Date());
				log.error(e.getMessage());
			}
		}
		return VERSION;
	}

	private boolean isProxyEnabled() {
		try {
			if (config != null) {
				return config.getJSONObject("proxy").getBoolean("enabled");
			}
		} catch (JSONException e) {
			log.error(e.getMessage());
		}
		return false;
	}

	public static void main(String[] args) {
		int port = 8282;
		URI configURI = null;

		if (args.length > 0) {
			configURI = new File(args[0]).toURI();
		}

		if (args.length > 1) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException nfe) {
				System.err.println(nfe.getMessage());
			}
		}

		if (configURI == null) {
			System.out.println("LDCache - http://entrystore.org/ldcache/");
			System.out.println("");
			System.out.println("Usage: ldc <path to configuration file> [listening port]");
			System.out.println("");
			System.exit(1);
		}

		Component component = new Component();
		component.getServers().add(Protocol.HTTP, port);
		component.getClients().add(Protocol.HTTP);
		component.getClients().add(Protocol.HTTPS);
		Context c = component.getContext().createChildContext();

		try {
			component.getDefaultHost().attach(new LDCache(c, configURI));
			component.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void configureLogging(JSONObject config) {
		BasicConfigurator.configure();
		Level l = Level.INFO;
		if (config.has("loglevel")) {
			try {
				l = Level.toLevel(config.getString("loglevel"), Level.INFO);
			} catch (JSONException e) {
				log.error(e.getMessage());
			}
		}
		Logger.getRootLogger().setLevel(l);
		log.info("Log level set to " + l);
	}

	// TODO handle shutdown, i.e., repository, executor in cacheimpl, and probably other stuff

}