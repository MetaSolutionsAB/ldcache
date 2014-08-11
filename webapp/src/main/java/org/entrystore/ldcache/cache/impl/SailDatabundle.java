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

import org.apache.log4j.Logger;
import org.entrystore.ldcache.cache.Databundle;
import org.entrystore.ldcache.util.Properties;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import java.util.Date;
import java.util.Set;

/**
 * @author Hannes Ebner
 */
public class SailDatabundle implements Databundle {

	Logger log = Logger.getLogger(SailDatabundle.class);

	Repository repository;

	URI databundleURI;

	Date lastModified;

	public SailDatabundle(Repository repository, URI databundleURI) {
		this.repository = repository;
		this.databundleURI = databundleURI;
	}

	@Override
	public URI getURI() {
		return databundleURI;
	}

	@Override
	public Date getModified() {
		if (lastModified == null) {
			RepositoryConnection rc = null;
			try {
				rc = repository.getConnection();
				RepositoryResult<Statement> rr = rc.getStatements(databundleURI, Properties.dctModified, null, false, databundleURI);
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
	public Set<URI> getResources() {
		if (databundleURI == null) {
			throw new IllegalArgumentException();
		}
		RepositoryConnection rc = null;
		Set<URI> result = null;
		try {
			rc = repository.getConnection();
			RepositoryResult<Statement> rr = rc.getStatements(databundleURI, Properties.ldcResource, null, false, databundleURI);
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
	public org.entrystore.ldcache.cache.Resource getResource(URI resourceURI) {
		if (resourceURI == null) {
			throw new IllegalArgumentException();
		}
		return RdfResource.loadFromRepository(repository, resourceURI);
	}

	@Override
	public void putResource(org.entrystore.ldcache.cache.Resource resource) {
		if (resource == null) {
			throw new IllegalArgumentException();
		}
		RdfResource.saveToRepository(repository, resource);
		updateModified(new Date());
	}

	@Override
	public void removeResource(URI resourceURI) {
		if (resourceURI == null) {
			throw new IllegalArgumentException();
		}
		RdfResource.removeFromRepository(repository, resourceURI);
		updateModified(new Date());
	}

	@Override
	public void delete() {
		for (URI r : getResources()) {
			RdfResource.removeFromRepository(repository, r);
		}
		synchronized (repository) {
			RepositoryConnection rc = null;
			try {
				rc = repository.getConnection();
				rc.begin();
				rc.remove((Resource) null, (URI) null, (Value) null, databundleURI);
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

	private void updateModified(Date lastModified) {
		if (lastModified == null) {
			throw new IllegalArgumentException();
		}
		synchronized (repository) {
			RepositoryConnection rc = null;
			try {
				rc = repository.getConnection();
				rc.begin();
				rc.remove(databundleURI, Properties.dctModified, null, databundleURI);
				rc.add(databundleURI, Properties.dctModified, Properties.getValueFactory().createLiteral(lastModified), databundleURI);
				rc.commit();
				this.lastModified = lastModified;
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

}