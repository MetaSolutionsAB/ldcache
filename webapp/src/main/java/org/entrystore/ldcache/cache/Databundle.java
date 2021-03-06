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

package org.entrystore.ldcache.cache;

import org.openrdf.model.Model;
import org.openrdf.model.URI;

import java.util.Date;
import java.util.Set;

/**
 * @author Hannes Ebner
 */
public interface Databundle {

	URI getURI();

	Date getModified();

	Set<URI> getResources();

	Resource getResource(URI uri);

	void putResource(Resource res);

	void removeResource(URI uri);

	void delete();

}