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

import org.entrystore.ldcache.cache.Cache;
import org.entrystore.ldcache.cache.Dataset;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;

import java.util.Set;

/**
 * @author Hannes Ebner
 */
public class SAILCache implements Cache {

	Repository repository;

	public SAILCache(Repository repository) {
		this.repository = repository;
	}

	@Override
	public Set<Dataset> getDatasets() {
		return null;
	}

	@Override
	public Dataset getDataset(URI uri) {
		return null;
	}

	@Override
	public void putDataset(URI uri, Dataset ds) {

	}

	@Override
	public void removeDataset(URI uri) {

	}

}