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
import org.entrystore.ldcache.cache.Resource;
import org.entrystore.ldcache.util.Properties;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import java.util.Date;

/**
 * @author Hannes Ebner
 */
public class RdfResource implements Resource {

	private static Logger log = Logger.getLogger(RdfResource.class);

	URI resourceURI;

	Model graph;

	Date modified;

	public RdfResource(URI resourceURI, Model graph, Date modified) {
		if (resourceURI == null || graph == null || modified == null) {
			throw new IllegalArgumentException();
		}
		this.resourceURI = resourceURI;
		this.graph = graph;
		this.modified = modified;
	}

	@Override
	public URI getURI() {
		return resourceURI;
	}

	@Override
	public Date getModified() {
		return modified;
	}

	@Override
	public void setModified(Date modified) {
		if (modified == null) {
			throw new IllegalArgumentException();
		}
		this.modified = modified;
	}

	@Override
	public Model getGraph() {
		return graph;
	}

	@Override
	public void setGraph(Model graph) {
		if (graph == null) {
			throw new IllegalArgumentException();
		}
		this.graph = graph;
		this.modified = new Date();
	}

	public static Resource loadFromRepository(Repository repository, URI resourceURI) {
		if (repository == null || resourceURI == null) {
			throw new IllegalArgumentException();
		}
		RepositoryConnection rc = null;
		Resource result = null;
		try {
			rc = repository.getConnection();
			// get graph
			RepositoryResult<Statement> rr = rc.getStatements(null, null, null, false, resourceURI);
			Model resource = Iterations.addAll(rr, new LinkedHashModel());
			// get modification date
			rr = rc.getStatements(resourceURI, Properties.dctModified, null, false);
			if (rr.hasNext()) {
				Value modifiedValue = rr.next().getObject();
				if (modifiedValue instanceof Literal) {
					Date modified = ((Literal) modifiedValue).calendarValue().toGregorianCalendar().getTime();
					result = new RdfResource(resourceURI, resource, modified);
				}
			} else {
				log.error("Unable to load from repository (not found): " + resourceURI);
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

	public static void saveToRepository(Repository repository, Resource resource) {
		if (repository == null || resource == null) {
			throw new IllegalArgumentException();
		}
		synchronized (repository) {
			RepositoryConnection rc = null;
			try {
				rc = repository.getConnection();
				rc.begin();
				rc.remove((org.openrdf.model.Resource) null, (URI) null, (Value) null, resource.getURI());
				rc.add(resource.getGraph(), resource.getURI());
				rc.remove(resource.getURI(), Properties.dctModified, null);
				rc.add(resource.getURI(), Properties.dctModified, Properties.getValueFactory().createLiteral(resource.getModified()));
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
	}

	public static void removeFromRepository(Repository repository, URI resourceURI) {
		if (repository == null || resourceURI == null) {
			throw new IllegalArgumentException();
		}
		synchronized (repository) {
			RepositoryConnection rc = null;
			try {
				rc = repository.getConnection();
				rc.begin();
				rc.remove((org.openrdf.model.Resource) null, (URI) null, (Value) null, resourceURI);
				rc.remove(resourceURI, Properties.dctModified, null);
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
	}

	public static boolean hasResource(Repository repository, URI resourceURI) {
		if (repository == null || resourceURI == null) {
			throw new IllegalArgumentException();
		}
		RepositoryConnection rc = null;
		try {
			rc = repository.getConnection();
			return rc.hasStatement(null, null, null, false, resourceURI);
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
		return false;
	}

}