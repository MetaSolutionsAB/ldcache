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

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Hannes Ebner
 */
public class ModelUtil {

	/**
	 * Converts a set of Resources to a set of URIs. Removes non-URIs, i.e., BNodes, Literals, etc.
	 *
	 * @param values A set of Values.
	 * @return A filtered and converted set of URIs.
	 */
	public static Set<URI> valueToURI(Set<Value> values) {
		Set<URI> result = new HashSet<>();
		for (Value v : values) {
			if (v != null && v instanceof URI) {
				result.add((URI) v);
			}
		}
		return result;
	}

	/**
	 * Converts a set of Resources to a set of URIs. Removes non-URIs, i.e., BNodes.
	 *
	 * @param resources A set of Resources.
	 * @return A filtered and converted set of URIs.
	 */
	public static Set<URI> resourceToURI(Set<Resource> resources) {
		Set<URI> result = new HashSet<>();
		for (Resource r : resources) {
			if (r != null && r instanceof URI) {
				result.add((URI) r);
			}
		}
		return result;
	}

	/**
	 * This method removes all statements with literals that do not match the whitelist.
	 *
	 * The algorithm is limited to a maximum of two levels from the root resource: it is checked for
	 * Literals and BNodes in object position and the root resources in subject position. If the object is a
	 * BNode (as it is the case with e.g. dcterms:description etc), one more step is made to find the literal
	 * and remove it together with all affected BNode statements.
	 *
	 * @param m The input model. Will not be modified.
	 * @param rootResource The starting point for the algorithm.
	 * @param languageWhitelist The languages that should not be removed from the model.
	 *                          The whitelist may contain "null" or be empty, which matches all literals
	 *                          without an explicit language set.
	 * @return A model that has been cleaned from language literals that do not match the whitelist.
	 */
	public static Model filterLanguageLiterals(Model m, URI rootResource, Set<String> languageWhitelist) {
		if (m == null || rootResource == null || languageWhitelist == null) {
			throw new IllegalArgumentException("Parameters must not be null");
		}
		if (languageWhitelist.contains("*")) {
			return m;
		}
		if (languageWhitelist.size() == 0 || languageWhitelist.contains("")) {
			// we don't want to modify the original list
			languageWhitelist = new HashSet<>(languageWhitelist);
			languageWhitelist.remove("");
			languageWhitelist.add(null);
		}
		Model result = new LinkedHashModel(m);
		Iterator<Statement> stmntIt = m.filter(rootResource, null, null).iterator();
		while (stmntIt.hasNext()) {
			Statement s = stmntIt.next();
			Value o = s.getObject();
			if (o instanceof Literal && !languageWhitelist.contains(((Literal) o).getLanguage())) {
				result.remove(s);
			} else if (o instanceof BNode) {
				Iterator<Statement> indirectStmntIt = m.filter((BNode) o, null, null).iterator();
				while (indirectStmntIt.hasNext()) {
					Statement s2 = indirectStmntIt.next();
					Value o2 = s2.getObject();
					if (o2 instanceof Literal && !languageWhitelist.contains(((Literal) o2).getLanguage())) {
						result.remove(s);
					}
				}
			}
		}
		return removeDanglingBNodes(result);
	}

	/**
	 * Removes all statements where a BNode appears in object position,
	 * but otherwise never appears in subject position.
	 *
	 * This method ignores the statements' context.
	 *
	 * @param model The input model. Will not be modified.
	 * @return A model from which dangling BNodes have been removed.
	 */
	private static Model removeDanglingBNodes(Model model) {
		if (model == null) {
			throw new IllegalArgumentException("Parameter must not be null");
		}
		Model result = new LinkedHashModel(model);
		for (Value object : model.objects()) {
			if (object instanceof BNode) {
				if (model.filter((BNode) object, null, null).size() == 0) {
					result.remove(null, null, object);
				}
			}
		}
		return result;
	}

}