[![Java CI with Maven](https://github.com/OLibutzki/axon/actions/workflows/maven.yml/badge.svg)](https://github.com/OLibutzki/axon/actions/workflows/maven.yml)
# Local EventStore and Embedded Server Draft
This repository acts as a first draft for a Local EventStore and an Embedded Server implementation.

## Sub-Pages
* [Local Event Store](axon-localeventstore/README.MD)
* [Embedded Server](axon-embeddedserver/README.MD)
* [Embedded Server Connector](axon-embeddedserver-connector/README.MD)
* [Examples](axon-example/README.MD)

## Local Event Store

The Local Event Store targets these Axon Framework issues:
- [Improve support for a local event store](https://github.com/AxonFramework/AxonFramework/issues/1342)
- [Consistency gap in conjunction with state-stored aggregates](https://github.com/AxonFramework/AxonFramework/issues/1103)

The motivation is explained in the referenced issues so it shouldn't be necessary to repeat it right here.

Detailed information how the draft works is provided in the [module's readme](axon-localeventstore/README.MD).

## Embedded Server

The Embedded Server target this Axon Framework issue:
- [Improve support for hierarchical Spring application contexts](https://github.com/AxonFramework/AxonFramework/issues/1341)

Detailed information how the draft works is provided in the modules' readme:
* [Embedded Server](axon-embeddedserver/README.MD)
* [Embedded Server Connector](axon-embeddedserver-connector/README.MD)

## Examples
The Examples module provides some integration tests in order to demonstrate and verfy the behaviour.

Please consult the [example docs](axon-example/README.MD).
