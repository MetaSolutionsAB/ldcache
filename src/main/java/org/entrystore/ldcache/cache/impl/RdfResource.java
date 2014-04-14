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

package org.entrystore.ldcache.cache.impl;

import org.entrystore.ldcache.cache.Resource;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;

import java.util.Date;

/**
 * @author Hannes Ebner
 */
public class RdfResource implements Resource {

	URI resourceURI;

	Model graph;

	Date modified;

	public RdfResource(URI resourceURI, Model graph, Date modified) {
		if (resourceURI == null || graph == null || modified == null) {
			throw new IllegalArgumentException();
		}
		this.resourceURI = resourceURI;
		this.graph = graph;
		this.modified = modified;
	}

	@Override
	public URI getURI() {
		return null;
	}

	@Override
	public Date getModified() {
		return modified;
	}

	@Override
	public void setModified(Date modified) {
		this.modified = modified;
	}

	@Override
	public Model getGraph() {
		return graph;
	}

	@Override
	public void setGraph(Model graph) {
		this.graph = graph;
		this.modified = new Date();
	}

	// FIXME stuff below in util instead?

	static Resource loadFromRepository(Repository repository, URI resourceURI) {
		return null; // FIXME
	}

	static void saveToRepository(Repository repository, Resource resource) {
		// FIXME
	}

}