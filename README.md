# reactive-user-orders-service

Service represents endpoint which responds with the stream (application/x-ndjson) of entities (user's order).
Entities are added to response upon readiness.

User info is retrieved from Mongo DB using reactive driver.
In order to compose entities service makes calls to upstream services with the help of Spring WebFlux's WebClient in a reactive manner.


Endpoint accepts `requestId` header whose value then propagated into reactive context in order to be logged.
