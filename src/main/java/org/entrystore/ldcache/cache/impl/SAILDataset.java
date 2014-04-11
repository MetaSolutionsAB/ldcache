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

import info.aduna.iteration.Iterations;
import org.apache.log4j.Logger;
import org.entrystore.ldcache.cache.Dataset;
import org.entrystore.ldcache.util.NS;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import java.util.Date;
import java.util.Set;

/**
 * @author Hannes Ebner
 */
public class SAILDataset implements Dataset {

	Logger log = Logger.getLogger(SAILDataset.class);

	Repository repository;

	URI uri;

	Date lastModified;

	ValueFactory valueFactory;

	public SAILDataset(Repository repository, URI uri) {
		this.repository = repository;
		this.valueFactory = repository.getValueFactory();
		this.uri = uri;
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public Date getModified() {
		if (lastModified == null) {
			RepositoryConnection rc = null;
			try {
				rc = repository.getConnection();
				RepositoryResult<Statement> rr = rc.getStatements(uri, valueFactory.createURI(NS.dcterms, "modified"), null, false);
				if (rr.hasNext()) {
					Statement result = rr.next();
					if (result.getObject() instanceof Literal) {
						Literal l = (Literal) result.getObject();
						lastModified = l.calendarValue().toGregorianCalendar().getTime();
					}
				}
			} catch (RepositoryException e) {
				log.error(e.getMessage());
			} finally {
				if (rc != null) {
					try {
						rc.close();
					} catch (RepositoryException e) {
						log.error(e.getMessage());
					}
				}
			}
		}
		return lastModified;
	}

	@Override
	public Set<URI> getGraphs() {
		if (uri == null) {
			throw new IllegalArgumentException();
		}
		RepositoryConnection rc = null;
		Set<URI> result = null;
		try {
			rc = repository.getConnection();
			RepositoryResult<Statement> rr = rc.getStatements(uri, valueFactory.createURI(NS.ldc, "hasGraph"), null, false, uri);
			while (rr.hasNext()) {
				Statement s = rr.next();
				if (s.getObject() instanceof URI) {
					result.add((URI) s.getObject());
				}
			}
		} catch (RepositoryException e) {
			log.error(e.getMessage());
		} finally {
			if (rc != null) {
				try {
					rc.close();
				} catch (RepositoryException e) {
					log.error(e.getMessage());
				}
			}
		}
		return result;
	}

	@Override
	public Model getGraph(URI uri) {
		if (uri == null) {
			throw new IllegalArgumentException();
		}
		RepositoryConnection rc = null;
		Model result = null;
		try {
			rc = repository.getConnection();
			RepositoryResult<Statement> rr = rc.getStatements(null, null, null, false, uri);
			result = Iterations.addAll(rr, new LinkedHashModel());
		} catch (RepositoryException e) {
			log.error(e.getMessage());
		} finally {
			if (rc != null) {
				try {
					rc.close();
				} catch (RepositoryException e) {
					log.error(e.getMessage());
				}
			}
		}
		return result;
	}

	@Override
	public void putGraph(URI uri, Model graph) {
		if (uri == null || graph == null) {
			throw new IllegalArgumentException();
		}
		RepositoryConnection rc = null;
		try {
			rc = repository.getConnection();
			rc.begin();
			rc.add(graph, uri);
			updateLastModified(rc);
			rc.commit();
		} catch (RepositoryException e) {
			try {
				rc.rollback();
			} catch (RepositoryException re) {
				log.error(re.getMessage());
			}
			log.error(e.getMessage());
		} finally {
			if (rc != null) {
				try {
					rc.close();
				} catch (RepositoryException e) {
					log.error(e.getMessage());
				}
			}
		}
	}

	@Override
	public void removeGraph(URI uri) {
		if (uri == null) {
			throw new IllegalArgumentException();
		}
		RepositoryConnection rc = null;
		try {
			rc = repository.getConnection();
			rc.begin();
			rc.remove((Resource) null, (URI) null, (Value) null, uri);
			rc.commit();
		} catch (RepositoryException e) {
			try {
				rc.rollback();
			} catch (RepositoryException re) {
				log.error(re.getMessage());
			}
			log.error(e.getMessage());
		} finally {
			if (rc != null) {
				try {
					rc.close();
				} catch (RepositoryException e) {
					log.error(e.getMessage());
				}
			}
		}
	}

	private void updateLastModified(RepositoryConnection rc) throws RepositoryException {
		this.lastModified = new Date();
		URI dateProp = valueFactory.createURI(NS.dcterms, "modified");
		rc.remove(uri, dateProp, null);
		rc.add(uri, dateProp, valueFactory.createLiteral(this.lastModified));
	}

	// FIXME we need last modified for graphs AND dataset

}