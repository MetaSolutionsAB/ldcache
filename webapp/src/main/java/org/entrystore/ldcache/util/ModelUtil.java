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

package org.entrystore.ldcache.util;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Hannes Ebner
 */
public class ModelUtil {

	public static Set<URI> valueToURI(Set<Value> values) {
		Set<URI> result = new HashSet<>();
		for (Value v : values) {
			if (v != null && v instanceof URI) {
				result.add((URI) v);
			}
		}
		return result;
	}

	public static Set<URI> resourceToURI(Set<Resource> resources) {
		Set<URI> result = new HashSet<>();
		for (Resource r : resources) {
			if (r != null && r instanceof URI) {
				result.add((URI) r);
			}
		}
		return result;
	}

}