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

import org.entrystore.ldcache.cache.Dataset;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;

import java.util.Date;
import java.util.Set;

/**
 * @author Hannes Ebner
 */
public class SAILDataset implements Dataset {

	Repository repository;

	public SAILDataset(Repository repository) {
		this.repository = repository;
	}

	@Override
	public URI getURI() {
		return null;
	}

	@Override
	public void setURI(URI uri) {

	}

	@Override
	public Date getModified() {
		return null;
	}

	@Override
	public Set<URI> getGraphs() {
		return null;
	}

	@Override
	public Model getGraph(URI uri) {
		return null;
	}

	@Override
	public void putGraph(URI uri, Model graph) {

	}

	@Override
	public void removeGraph(URI uri) {

	}
}
