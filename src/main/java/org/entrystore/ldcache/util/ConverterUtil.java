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
import org.openrdf.model.Graph;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.helpers.StatementCollector;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * @author Hannes Ebner
 */
public class ConverterUtil {

	static Logger log = Logger.getLogger(ConverterUtil.class);

	/**
	 * @param graph
	 *            The Graph to be serialized.
	 * @param writer
	 *            One of the following: N3Writer, NTriplesWriter,
	 *            RDFXMLPrettyWriter, RDFXMLWriter, TriGWriter, TriXWriter,
	 *            TurtleWriter
	 * @return A String representation of the serialized Graph.
	 */
	public static String serializeGraph(Graph graph, Class<? extends RDFWriter> writer) {
		if (graph == null || writer == null) {
			throw new IllegalArgumentException("Parameters must not be null");
		}

		StringWriter stringWriter = new StringWriter();
		RDFWriter rdfWriter = null;
		try {
			Constructor<? extends RDFWriter> constructor = writer.getConstructor(Writer.class);
			rdfWriter = (RDFWriter) constructor.newInstance(stringWriter);
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		if (rdfWriter == null) {
			return null;
		}

		try {
			Map<String, String> namespaces = NS.getMap();
			for (String nsName : namespaces.keySet()) {
				rdfWriter.handleNamespace(nsName, namespaces.get(nsName));
			}
			rdfWriter.startRDF();
			for (Statement statement : graph) {
				rdfWriter.handleStatement(statement);
			}
			rdfWriter.endRDF();
		} catch (RDFHandlerException rdfe) {
			log.error(rdfe.getMessage());
		}
		return stringWriter.toString();
	}

	public static void serializeGraph(Graph graph, RDFWriter rdfWriter) {
		if (graph == null || rdfWriter == null) {
			throw new IllegalArgumentException("Parameters must not be null");
		}
		try {
			Map<String, String> namespaces = NS.getMap();
			for (String nsName : namespaces.keySet()) {
				rdfWriter.handleNamespace(nsName, namespaces.get(nsName));
			}
			rdfWriter.startRDF();
			for (Statement statement : graph) {
				rdfWriter.handleStatement(statement);
			}
			rdfWriter.endRDF();
		} catch (RDFHandlerException rdfe) {
			log.error(rdfe.getMessage());
		}
	}

	/**
	 * @param serializedGraph
	 *            The Graph to be deserialized.
	 * @param parser
	 *            Instance of the following: N3Parser, NTriplesParser,
	 *            RDFXMLParser, TriGParser, TriXParser, TurtleParser
	 * @return A String representation of the serialized Graph.
	 */
	public static Graph deserializeGraph(String serializedGraph, RDFParser parser) {
		if (serializedGraph == null || parser == null) {
			throw new IllegalArgumentException("Parameters must not be null");
		}

		StringReader reader = new StringReader(serializedGraph);
		StatementCollector collector = new StatementCollector();
		try {
			parser.setRDFHandler(collector);
			parser.parse(reader, "");
		} catch (RDFHandlerException rdfe) {
			log.error(rdfe.getMessage());
		} catch (RDFParseException rdfpe) {
			log.error(rdfpe.getMessage());
		} catch (IOException ioe) {
			log.error(ioe.getMessage());
		}

		return new GraphImpl(collector.getStatements());
	}

}