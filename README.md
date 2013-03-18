Hard-wired load generator for the wager service. To run from the command line, do:

`mvn package`

`java -jar target/wager-service-load-generator-1.0-SNAPSHOT.jar`

For command line parameters, see the `Loader.parse(String[] args)` method.

Notes:
* For as long as the service is currently synchronous, the number of requests per second a single load generator thread
can deliver is probably going to be limited by the single-request latency. That means that the only way to generate significant
loads is by increasing the number of threads using the command-line parameter (or by changing the default).
