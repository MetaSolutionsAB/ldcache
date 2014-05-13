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

import org.openrdf.rio.RDFFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hannes Ebner
 */
public class RdfMedia {

	public static List<Preference<MediaType>> RDF_FORMATS = new ArrayList<Preference<MediaType>>();

	public static List<MediaType> SUPPORTED_MEDIA_TYPES = new ArrayList<MediaType>();

	static {
		RDF_FORMATS.add(new Preference(MediaType.APPLICATION_RDF_XML));
		RDF_FORMATS.add(new Preference(MediaType.APPLICATION_JSON));
		RDF_FORMATS.add(new Preference(MediaType.TEXT_RDF_N3));
		RDF_FORMATS.add(new Preference(new MediaType(RDFFormat.TURTLE.getDefaultMIMEType())));
		RDF_FORMATS.add(new Preference(new MediaType(RDFFormat.TRIX.getDefaultMIMEType())));
		RDF_FORMATS.add(new Preference(new MediaType(RDFFormat.NTRIPLES.getDefaultMIMEType())));
		RDF_FORMATS.add(new Preference(new MediaType(RDFFormat.TRIG.getDefaultMIMEType())));
		RDF_FORMATS.add(new Preference(new MediaType(RDFFormat.JSONLD.getDefaultMIMEType())));
		RDF_FORMATS.add(new Preference(new MediaType(RDFFormat.RDFJSON.getDefaultMIMEType())));
		for (Preference<MediaType> p : RDF_FORMATS) {
			SUPPORTED_MEDIA_TYPES.add(p.getMetadata());
		}
	}

}