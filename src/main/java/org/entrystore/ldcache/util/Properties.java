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

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 * @author Hannes Ebner
 */
public class Properties {

	private static ValueFactory valueFactory;

	public static URI modified;

	public static URI resource;

	static {
		valueFactory = new ValueFactoryImpl();
		modified = valueFactory.createURI(NS.dcterms, "modified");
		resource = valueFactory.createURI(NS.ldc, "resource");
	}

	public static ValueFactory getValueFactory() {
		return valueFactory;
	}

}