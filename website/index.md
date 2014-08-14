# LDCache - a Linked Data Cache [![Build Status](https://drone.io/bitbucket.org/metasolutions/ldcache/status.png)](https://drone.io/bitbucket.org/metasolutions/ldcache/latest)

LDCache is a minimum footprint caching server for Linked (Open) Data.

## Installation

LD Cache can be built using Maven by running in the project's root directory: `mvn build`

The `webapp` package can be used for container deployments, e.g. Tomcat.

The `standalone` package contains the folder `standalone/target/dist` with an executable and all dependency libraries.

Run `bin/ldc <config> [port]` to execute. 

## Configuration

### repository

#### type

Currently `native` (requires a configured URI, see next setting) and `memory` are supported, both are Sesame-native. Additional repository backends will be supported soon.

#### uri

The URI of the triple store. Currently only `file:///` URIs are supported for native stores.

#### indexes

Which triple store indexes should be maintained. All combinations of c, s, p and o are possible. For now it is recommended to use "cspo", which also should suffice.

### cache

#### rateLimit

This value limits the number of requests per server per second. E.g., a rateLimit of 10 limits the amount of requests to 10 per server per second. This is to avoid situations similar to DoS.

#### threadPoolSize

The size of the thread pool for caching data. Currently not used.

#### requestTimeOut

The timeout value is set in milliseconds for all possible timeout configurations, i.e., this value is set for socket, connection, etc timeouts. A more fine-grained configuration may be implemented if necessary.

### proxy

Determines whether the proxy service should be enabled. The proxy service bypasses the cache and allows arbitrary RDF resources to be fetched. Like with the cache, format conversions are handled transparently through normal content negotiation between proxy and data source and proxy and client.

### databundles

The properties in the section are used to specify databundles, i.e., which datasets/resources are to be cached, which links are supposed to be followed etc. Most of the properties may also be used as request parameters.

#### name

The name of the data set. No specific rules apply for this value.

#### resources

Which resources to cache. In combination with the other parameters this allows for quite flexible configuration. In the example below only one resource is provided, but it acts as an index resource because all pointed out objects are followed. An alternative would be to provide a list of all resources that the index resource points out (which doesn't scale that well in case the list of resources changes etc).

#### follow

A comma-separated list of predicates. The objects of matching triples are followed during graph traversal.

#### followTuples

A list of predicate-object tuples that have to match in order for the subject to be followed. Useful for index-resources.

#### includeDestinations

A whitelist with prefixes of destinations to include when traversing the graph.

#### followDepth

The maximum depth of the graph traversal.

### Logging

The log level can be set through "loglevel". Available levels of detail are: debug, info, warn, error.

### Example

Find a ready to use example configuration in `ldcache.json_example` in `webapp/src/main/resources/`.

```
{
    "repository": {
        "type": "memory",
        "uri": "file:///srv/ldcache/repository/",
        "indexes": "cspo,posc,spoc"
    },
    "cache": {
        "rateLimit": 5,
        "threadPoolSize": 5,
        "requestTimeout": 10000
    },
    "proxy": {
        "enabled": false
    },
    "loglevel": "info",
    "databundles": [
        {
            "name": "dbpedia-laureates",
            "resources": [
                "http://data.nobelprize.org/all/laureate",
                "http://data.nobelprize.org/all/nobelprize"
            ],
            "follow": [
                "http://dbpedia.org/ontology/affiliation",
                "http://dbpedia.org/ontology/birthPlace",
                "http://dbpedia.org/ontology/deathPlace",
                "owl:sameAs"
            ],
            "followTuples": [
                { "rdf:type": "http://data.nobelprize.org/terms/Laureate" },
                { "rdf:type": "http://data.nobelprize.org/terms/NobelPrize" }
            ],
            "includeDestinations": [
                "http://dbpedia.org/resource/",
                "http://data.nobelprize.org"
            ],
            "followDepth": 3
        }
    ]
}
```

## Usage (API)

### Cached resources

The cache resource requires that the requested RDF resource (and subsequent resources if links are to be followed) is cached in the local LDCache repository. See the configuration section above for instructions on how to configure LDCache to prefetch RDF resources.

Request URL: `http://ldc-server/`

Available parameters:

* `format`: A valid RDF MIME type (see below under "Content negotiaton" for a list of supported MIME types). Overrides an eventually supplied `Accept` header.
* `url`: The RDF resource to be fetched.
* `follow`: A comma-separated list of predicates. The objects of matching triples are followed.
* `followTuples`: A comma-separated list of predicate-object tuples in the format `predicate|object`. The subjects of matching triples are followed. Useful for index-resources.
* `includeDestinations`: A comma-separated whitelist with prefixes of destinations to include when traversing the graph.
* `followDepth`: The maximum distance from the root-resource that should be followed. Default is 0, i.e., only the resource identified by the `url` parameter will be fetched and no links are followed.

Some of the parameters allow to provide a URI in which the following namespace definitions may be used:

* `dc`: `http://purl.org/dc/elements/1.1/`
* `dcterms`, `dct`: `http://purl.org/dc/terms/`
* `foaf`: `http://xmlns.com/foaf/0.1/`
* `rdf`: `http://www.w3.org/1999/02/22-rdf-syntax-ns#`
* `rdfs`: `http://www.w3.org/2000/01/rdf-schema#`
* `xsd`: `http://www.w3.org/2001/XMLSchema#`
* `skos`: `http://www.w3.org/2004/02/skos/core#`
* `owl`: `http://www.w3.org/2002/07/owl#`
* `vcard`: `http://www.w3.org/2001/vcard-rdf/3.0#`
* `es`: `http://entrystore.org/terms/`

### Uncached resources

The proxy resource is still quite simple as it only proxies one resource unlike the smartness that the cache (see above) provides. The proxy has to be activated in the configuration.

Request URL: `http://ldc-server/proxy`

Available parameters:

* `format`: A valid RDF MIME type (see below under "Content negotiaton" for a list of supported MIME types). Overrides an eventually supplied `Accept` header.
* `url`: The RDF resource to be proxied.

### Content negotiation

All content negotiation is handled transparently. During the caching process LDCache negotiates with the RDF source; when a client requests a cached resource from LDCache any RDF format supported by Sesame Rio plus JSON-LD is supported. This also means that LDCache may be used as transparent RDF converter in case an RDF source does not support the desired RDF format directly.

Supported RDF formats and their MIME types:

* JSON-LD: `application/ld+json`
* RDF/JSON: `application/rdf+json`
* RDF/XML: `application/rdf+xml`
* N3: `text/n3`
* Turtle: `text/turtle`
* Trix: `application/trix`
* N-Triples: `text/plain`
* Trig: `application/x-trig`

### Examples

#### Cache

* [`http://ldc/?url=http://dbpedia.org/resource/Marie_Curie&follow=http://dbpedia.org/ontology/doctoralStudent&followDepth=2`](http://ldc/?url=http://dbpedia.org/resource/Marie_Curie&follow=http://dbpedia.org/ontology/doctoralStudent&followDepth=2)
* [`http://ldc/?url=http://data.nobelprize.org/all/laureate&followTuples=rdf:type|http://data.nobelprize.org/terms/Laureate&follow=dbp:spouse&followDepth=3`](http://ldc/?url=http://data.nobelprize.org/all/laureate&followTuples=rdf:type|http://data.nobelprize.org/terms/Laureate&follow=dbp:spouse&followDepth=3)

#### Proxy

* [`http://ldc/proxy?url=http://dbpedia.org/resource/Henri_Becquerel`](http://ldc/proxy?url=http://dbpedia.org/resource/Henri_Becquerel)

## Roadmap, feature requests and bugs

Please use the issue tracker at [https://metasolutions.atlassian.net/browse/LDC](https://metasolutions.atlassian.net/browse/LDC) to submit feature requests and bugs.

The roadmap is generated out of the issues in JIRA, see [https://metasolutions.atlassian.net/browse/LDC/?selectedTab=com.atlassian.jira.jira-projects-plugin:roadmap-panel](the roadmap section) for details.

## Background

Initial development was carried out within a [VINNOVA-funded project](http://metasolutions.se/projects/vidareutveckling-av-lankade-oppna-data-for-nobelpris/) for opening up information about Nobel Prizes and Nobel Laureates as Linked Open Data.

## License

[MetaSolutions AB](http://www.metasolutions.se) licenses this work under the terms of the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) (the "License"); you may not use this file except in compliance with the License. See the `LICENSE.txt` file distributed with this work for the full License.
