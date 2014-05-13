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

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Hannes Ebner
 */
public class JsonUtil {

	static Logger log = Logger.getLogger(JsonUtil.class);

	public static Set<Value> jsonArrayToSet(JSONArray array) {
		Set<Value> result = new HashSet<Value>();
		for (int i = 0; i < array.length(); i++) {
			String uri = null;
			try {
				uri = array.getString(i);
			} catch (JSONException e) {
				log.warn(e.getMessage());
				continue;
			}
			if (uri != null) {
				result.add(new URIImpl(uri));
			}
		}
		return result;
	}

}