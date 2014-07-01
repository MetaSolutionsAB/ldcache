# LD Cache

LD Cache is a minimum footprint caching server for Linked (Open) Data.

## Installation

LD Cache can be built using Maven by running in the project's root directory: `mvn build`

The `webapp` package can be used for container deployments, e.g. Tomcat.

The `standalone` package contains the folder `standalone/target/dist` with an executable and all dependency libraries. Run `bin/ldc <config> [port]` to execute. 

## Usage (API)

### Cached resources

TODO

### Uncached resources

TODO

### Content negotiation

TODO

### Examples

TODO

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

### datasets

The properties in the section are used to specify datasets, i.e., which resources are to be cached, which links are supposed to be followed etc.

#### name

The name of the data set. No specific rules apply for this value.

#### resources

Which resources to cache. In combination with the other parameters this allows for quite flexible configuration. In the example below only one resource is provided, but it acts as an index resource because all pointed out objects are followed. An alternative would be to provide a list of all resources that the index resource points out (which doesn't scale that well in case the list of resources changes etc).

#### follow

Which predicates to follow when traversing the graph.

#### followTuples

A list of predicate-object tuples that have to match in order for the resource to be cached.

#### includeDestinations

A whitelist with prefixes of destinations to include when traversing the graph.

#### followDepth

The maximum depth of the graph traversal.

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
        "rateLimit": 10,
        "threadPoolSize": 5,
        "requestTimeout": 10000
    },
    "proxy": {
        "enabled": false
    },
    "datasets": [
        {
            "name": "dbpedia-laureates",
            "resources": [
                "http://data.nobelprize.org/all/laureate"
            ],
            "follow": [
                "http://dbpedia.org/ontology/affiliation",
                "http://dbpedia.org/ontology/birthPlace",
                "http://dbpedia.org/ontology/deathPlace",
                "owl:sameAs"
            ],
            "followTuples": {
                "rdf:type": "http://data.nobelprize.org/terms/Laureate"
            },
            "includeDestinations": [
                "http://dbpedia.org/resource/",
                "http://data.nobelprize.org"
            ],
            "followDepth": 3
        }
    ]
}
```

## Roadmap, feature requests and bugs

Please use the issue tracker at [https://metasolutions.atlassian.net/browse/LDC](https://metasolutions.atlassian.net/browse/LDC) to submit feature requests and bugs.

The roadmap is generated out of the issues in JIRA, see [https://metasolutions.atlassian.net/browse/LDC/?selectedTab=com.atlassian.jira.jira-projects-plugin:roadmap-panel](the roadmap section) for details.

## Background

Initial development was carried out within a [VINNOVA-funded project](http://metasolutions.se/projects/vidareutveckling-av-lankade-oppna-data-for-nobelpris/) for opening up information about Nobel Prizes and Nobel Laureates as Linked Open Data.

## License

MetaSolutions AB licenses this work under the terms of the Apache License 2.0 (the "License"); you may not use this file except in compliance with the License. See the `LICENSE.txt` file distributed with this work for the full License.
