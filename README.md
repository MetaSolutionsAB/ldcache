# LD Cache

LD Cache is a minimum footprint caching server for Linked (Open) Data.

Initial development was carried out within a [VINNOVA-funded project](http://metasolutions.se/projects/vidareutveckling-av-lankade-oppna-data-for-nobelpris/) for opening up information about Nobel Prizes and Nobel Laureates as Linked Open Data.

## Installation

LD Cache can be built using Maven by running in the project's root directory: `mvn build`

The `webapp` package can be used for container deployments, e.g. Tomcat.

The `standalone` package contains the folder `standalone/target/dist` with an executable and all dependency libraries. Run `bin/ldc <config> [port]` to execute. 

## Configuration

The available configuration options will be documented soon, until then have a look at the example configuration in `ldcache.json_example` in `webapp/src/main/resources/`.

## License

MetaSolutions AB licenses this work under the terms of the Apache License 2.0 (the "License"); you may not use this file except in compliance with the License. See the `LICENSE.txt` file distributed with this work for the full License.