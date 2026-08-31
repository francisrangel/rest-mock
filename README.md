# rest-mock

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17%2B-blue.svg)](#install)
[![Maven Central](https://img.shields.io/maven-central/v/org.simulatest/restmock?label=maven%20central)](https://central.sonatype.com/artifact/org.simulatest/restmock)

**The simplest way to mock HTTP in JVM**

- No config  
- No DSL  
- No server setup headaches  

Just this:

```java
RestMock.whenGet("/users/42").thenReturnJSON("{\"name\":\"Bob\"}");
RestMock.startServer();
```

That’s it.

---

## Install

```xml
<dependency>
  <groupId>org.simulatest</groupId>
  <artifactId>restmock</artifactId>
  <version>0.2.0</version>
  <scope>test</scope>
</dependency>
```

Gradle:

```groovy
testImplementation 'org.simulatest:restmock:0.2.0'
```

Requires Java 17 or later.

The JUnit dependency is optional: you only need it for `RestMockExtension`, and
if you use that you already have JUnit. On the module path the artifact is
`org.simulatest.restmock`.

---

## Quick example

A complete test, start to finish:

```java
class UserServiceTest {

    record Person(String name) {}

    @RegisterExtension
    static RestMockExtension server = new RestMockExtension();

    @Test
    void fetchesAUser() throws Exception {
        RestMock.whenGet("/users/42").thenReturnJSON("{\"name\":\"Bob\"}");
        RestMock.whenGet("/users/43").thenReturnJSON(new Person("John"));

        var users = new UserService(RestMock.baseUrl());

        assertEquals("Bob", users.byId(42).name());
        assertEquals(1, RestMock.requests().countForPath("/users/42"));
    }
}
```

```
GET /users/42 → {"name":"Bob"}
GET /users/43 → {"name":"John"}
```

The extension starts the server before the class and clears routes between
tests, so nothing leaks from one test to the next. `RestMock.baseUrl()` is the
address to point your client at.

---

## Why this exists

Most HTTP mocking libraries start simple…  
and turn into frameworks.

- dozens of config options  
- verbose DSLs  
- JSON files everywhere  
- hard to read, harder to maintain  

If you’ve used WireMock or MockServer, you know the deal.

**rest-mock goes the opposite direction:**

> Write the mock inline, next to your test, in one line.

---

## The core idea

Mocking an endpoint should feel like writing a return statement.

```java
RestMock.whenGet("/users/{id}")
        .thenReturnJSON("{\"id\":\"${id}\"}");
```

Call it:

```
GET /users/42 → {"id":"42"}
```

No matchers.  
No request builders.  
No ceremony.

---

## What you get

Everything you actually need, nothing you don’t:

- All HTTP verbs: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS  
- HEAD and OPTIONS answered from the routes you already stubbed  
- Path templates: `/users/{id}`  
- Dynamic responses: `${id}`, `${query}`, `${body}`  
- JSON, XML, HTML, text  
- Load responses from files  
- Custom status codes and headers  
- Response delays for timeout testing  
- Request inspection and counting  
- JUnit extension for automatic lifecycle  
- Built-in CORS support, preflight included  
- 404s that tell you which stub you missed  

And that’s it.

---

## Why developers like it

### 1. Tests stay readable

```java
RestMock.whenPost("/login")
        .thenReturnText("hello ${username}");
```

No jumping between files.  
No mental overhead.

---

### 2. Zero setup

Uses the JDK built-in HTTP server.

No Jetty.  
No Netty.  
No containers.

---

### 3. One mental model

Everything becomes `${name}`:

- path → `/users/{id}`
- query → `?id=42`
- body → `{ "id": 42 }`

Same access pattern everywhere.

---

### 4. Designed for speed

This is not a “full simulation framework”.

It’s for:
- fast unit tests  
- integration tests  
- mocking dependencies quickly  

If you need full API simulation or traffic proxying → use something else.

---

## Reference

Everything above is the whole library in five minutes. What follows is the
detail, in the order you are likely to need it.

---

## Dynamic data from requests

```java
RestMock.whenGet("/users/{id}")
        .thenReturnText("user ${id} aka ${nickname}");
```

```
GET /users/42?nickname=bob → "user 42 aka bob"
```

Works with:
- path params  
- query params  
- form body  
- JSON body  
- XML body  
- request headers, under a `header.` prefix  

Nested fields use dotted paths and array elements use indexes:

```java
RestMock.whenPost("/orders")
        .thenReturnText("first sku: ${items.0.sku} for ${customer.name}");
```

Headers are addressed under a `header.` prefix, never as a bare `${X-Tenant}`:

```java
RestMock.whenGet("/whoami").thenReturnText("tenant=${header.X-Tenant}");
```

The bare namespace holds what you wrote. `Host`, `User-Agent` and `Accept` are
attached by your HTTP client, not by your test, so keeping them out means a
typo can't quietly resolve to one of them instead of failing. Names are matched
case-insensitively, so `${header.X-Tenant}` finds the header however the server
canonicalized it.

When a name exists in more than one place the most specific wins: path captures,
then body fields, then query params. Headers have their own namespace and can't
collide with any of them.

A placeholder that matches nothing is a mistake in the stub, so it fails instead
of shipping `${nmae}` to your client and letting an assertion on the status code
pass anyway:

```
500  No value for ${nmae}. Available names: id, nickname; plus 4 request headers as ${header.NAME}
```

The names you wrote are listed; the headers are counted. An ordinary request
carries five to ten of them, and spelling them all out buried the one name you
were actually looking for.

Values are escaped for the format you're returning, so a request can't break the
document it lands in:

```java
RestMock.whenGet("/users/{id}").thenReturnJSON("{\"id\":\"${id}\"}");
```

```
GET /users/a"b → {"id":"a\"b"}
```

JSON gets string escaping, XML and HTML get entities, plain text is left alone.
If you need a body passed through untouched, `thenReturnFile` skips substitution
entirely.

---

## When a stub doesn't match

The slowest part of mocking HTTP is usually the same question: *why isn't my
mock matching?* An empty 404 answers none of it, so rest-mock puts the answer in
the body.

```java
RestMock.whenGet("/users/1").thenReturnJSON("{\"name\":\"Bob\"}");
RestMock.whenPost("/orders").thenReturnText("ok");
```

```
GET /users/01
->
404
No stub for GET /users/01

Closest stub: GET /users/1

Stubbed routes:
  GET     /users/1
  POST    /orders
```

Right verb, wrong path is one kind of mistake; right path, wrong verb is the
other, and it gets said outright:

```
GET /orders
->
404
/orders is stubbed for POST, not GET.
```

Path templates are offered back exactly as you wrote them, braces included, so
the hint names the line you can go and fix:

```
GET /user/42  ->  Closest stub: GET /users/{id}
```

And when nothing is stubbed at all, the report is the call you forgot:

```
No stub for GET /users/1

Nothing is stubbed. Call RestMock.whenGet("/users/1").thenReturn... before the request.
```

---

## Stubs that could never match are rejected

A stub URI is a path. Anything else fails at the `when*` line that wrote it,
instead of turning into a route no request can reach:

```java
RestMock.whenGet("/users?active=true");
// Stub URI "/users?active=true" must not contain a query string; stub the path
// "/users" instead and read the query with ${name} or from RestMock.requests().

RestMock.whenGet("users/1");
// Stub URI "users/1" must start with '/'.

RestMock.whenGet("/users/{id");
// Stub URI "/users/{id" has an unclosed '{'; path placeholders look like /users/{id}.
```

That last one was the worst of the three: a template missing its closing brace
used to compile into a literal path with a brace in it, match nothing, and look
completely correct in the test.

---

## Loading responses from files

When a response is too large or complex to inline, you can load it from a file in your test resources folder (`src/test/resources`):

```java
RestMock.whenGet("/invoice")
        .thenReturnJSONFromResource("invoice.json");

RestMock.whenGet("/report")
        .thenReturnXMLFromResource("report.xml");

RestMock.whenGet("/page")
        .thenReturnHTMLFromResource("page.html");

RestMock.whenGet("/readme")
        .thenReturnTextFromResource("readme.txt");
```

Place the file in `src/test/resources` and pass the filename. rest-mock loads it from the classpath and serves it with the matching content type. This keeps your test code short while the actual response payload lives in a dedicated file you can inspect and edit separately.

A missing file fails on the spot with the name it looked for. Nothing here throws a checked exception, so stubbing never forces a `try`/`catch` or a `throws` onto your test.

---

## Serving files (images, PDFs, binaries)

For non-text responses, use `thenReturnFile`. It serves bytes as-is, with no template substitution and no UTF-8 round-trip:

```java
RestMock.whenGet("/logo")
        .thenReturnFileFromResource("logo.png");
```

The MIME type is inferred from the filename extension (`.png` to `image/png`, `.pdf` to `application/pdf`, `.zip` to `application/zip`, etc.); unknown extensions fall back to `application/octet-stream`. Override it when you need a specific MIME:

```java
RestMock.whenGet("/data")
        .thenReturnFileFromResource("payload.bin", "application/x-protobuf");
```

When the bytes come from somewhere other than a classpath file (generated, fetched, hand-crafted), pass them inline:

```java
byte[] pdf = generatePdf(invoice);

RestMock.whenGet("/invoice")
        .thenReturnFile(pdf, "application/pdf");
```

`thenReturnFile(byte[])` without a content type defaults to `application/octet-stream`, since raw bytes carry no filename to infer from.

The file methods bypass `${...}` substitution because templates require a string view of the content. For text responses with placeholders, stick with `thenReturnJSONFromResource`, `thenReturnTextFromResource`, etc.

---

## Custom status codes and headers

By default every response returns 200. You can change that with `withStatus()`:

```java
RestMock.whenPost("/users")
        .thenReturnJSON("{\"id\":1}")
        .withStatus(201);
```

This works with any content type, so you can return a JSON error body with the right status code:

```java
RestMock.whenPost("/users")
        .thenReturnJSON("{\"error\":\"email already taken\"}")
        .withStatus(422);
```

For simple error messages where you don't need a specific content type, there's a shorthand:

```java
RestMock.whenGet("/secret")
        .thenReturnErrorCodeWithMessage(403, "Forbidden");
```

Headers work the same way. Chain as many as you need:

```java
RestMock.whenGet("/api/data")
        .thenReturnJSON("{\"items\":[]}")
        .withStatus(200)
        .withHeader("Cache-Control", "no-cache")
        .withHeader("X-Request-Id", "abc123");
```

---

## Simulating slow responses

Need to test timeouts, retries, or loading states? Use `withDelay()` to make a route wait before responding:

```java
RestMock.whenGet("/slow-api")
        .thenReturnJSON("{\"data\":\"here\"}")
        .withDelay(2000);
```

The server will wait 2 seconds before sending the response. This is useful for verifying that your HTTP client handles timeouts correctly:

```java
RestMock.whenGet("/unreliable")
        .thenReturnText("too late")
        .withDelay(5000)
        .withStatus(200);
```

If milliseconds are ambiguous where you're reading it, say the unit out loud:

```java
RestMock.whenGet("/slow-api")
        .thenReturnJSON("{\"data\":\"here\"}")
        .withDelay(Duration.ofSeconds(2));
```

Delay chains with `withStatus()` and `withHeader()` like everything else. Routes without `withDelay()` respond immediately, and a negative delay is rejected rather than ignored.

---

## Inspecting received requests

rest-mock records every request the server receives. After your test code runs, you can inspect what was actually called through `RestMock.requests()`:

```java
RestMock.whenPost("/api/users").thenReturnJSON("{\"id\":1}").withStatus(201);

// ... your code makes HTTP calls ...

// How many requests hit /api/users?
RestMock.requests().countForPath("/api/users");

// What was the last POST body?
String body = RestMock.requests()
        .lastForPath("/api/users")
        .orElseThrow()
        .body();

// What did it send along with it?
ReceivedRequest last = RestMock.requests().lastForPath("/api/users").orElseThrow();

last.header("Content-Type");     // Optional<String>, case-insensitive
last.header("Authorization");
last.queryParam("dry_run");      // Optional<String>, URL-decoded

// Filter by method and path
List<ReceivedRequest> posts = RestMock.requests()
        .forRoute(HttpMethod.POST, "/api/users");

// Did anything hit this endpoint?
RestMock.requests().forPath("/health").isEmpty();
```

Each `ReceivedRequest` captures the method, path, query string, headers, body, and timestamp.

Read headers with `header(name)` rather than through the raw `headers()` map. The JDK HTTP server rewrites every header name to first-letter-uppercase, so a header you sent as `Content-Type` is stored under `Content-type` and an exact lookup for the name you sent finds nothing. `header()` and `headerValues()` are case-insensitive, as HTTP itself is. Query parameter names stay case-sensitive, and `queryParam()` decodes the value for you.

The `RequestLog` provides common filters out of the box:

- `all()`: every request in arrival order
- `forPath(path)`: literal path match
- `forMethod(method)`: filter by HTTP verb
- `forRoute(method, path)`: both at once
- `countForPath(path)`, `countForRoute(method, path)`: counts
- `last()`, `lastForPath(path)`: most recent
- `isEmpty()`: quick check

For anything more specific, `all()` gives you the raw list to filter however you want.

When an assertion about the log fails, `expected: <1> but was: <0>` tells you
nothing about the calls that were actually made. Pass the log itself as the
message and it prints them:

```java
assertEquals(1, RestMock.requests().countForPath("/orders"), RestMock.requests()::toString);
```

```
org.opentest4j.AssertionFailedError: 2 requests received:
  1. GET     /users/1
  2. POST    /order (13 chars)
==> expected: <1> but was: <0>
```

The request log is cleared automatically when you call `RestMock.clean()` or when the `RestMockExtension` cleans between tests.

---

## HEAD and OPTIONS come for free

Stub `GET` and you get `HEAD` on the same path: same status, same headers, the
body's length in `Content-Length`, and no body. `OPTIONS` is answered from
whatever the path actually serves:

```java
RestMock.whenGet("/users/1").thenReturnJSON("{\"name\":\"Bob\"}");
RestMock.whenDelete("/users/1").thenReturnText("gone");
```

```
HEAD    /users/1 → 200, Content-Length: 14, no body
OPTIONS /users/1 → 204, Allow: GET, HEAD, DELETE, OPTIONS
```

Stub them explicitly with `whenHead()` or `whenOptions()` when you want
something else; an explicit stub always wins.

---

## Browser-driven tests (CORS)

If the code under test runs in a browser, rest-mock answers the preflight for
you. Stub the route you actually care about; the `OPTIONS` call is handled from
whatever methods you registered for that path:

```java
RestMock.whenGet("/api/data").thenReturnJSON("{\"ok\":true}");
```

```
OPTIONS /api/data
  Origin: http://localhost:3000
  Access-Control-Request-Method: GET
->
  204
  Access-Control-Allow-Origin: http://localhost:3000
  Access-Control-Allow-Methods: GET, OPTIONS
  Access-Control-Allow-Credentials: true
```

The origin you send is echoed back rather than `*`, so credentialed requests
work, and `Access-Control-Request-Headers` is mirrored, so posting JSON with an
`Authorization` header passes preflight. Errors carry the headers too: a 404
reaches the browser as a readable 404 instead of an opaque CORS failure.

Requests without an `Origin` get no CORS headers at all, so plain JVM clients
see clean responses. An explicit `whenOptions()` stub supplies the body and
status, and still answers the preflight: stubbing a route never takes CORS away
from it.

---

## Customizing JSON and XML serialization

rest-mock uses Jackson under the hood. Records, POJOs, and getters just work:

```java
record Customer(String name, int age) {}

RestMock.whenGet("/me").thenReturnJSON(new Customer("Bob", 25));
```

Jackson auto-detects modules on your test classpath. Add `jackson-datatype-jsr310` to your test dependencies and `LocalDateTime` serializes as `"2026-05-03T18:58:12"` instead of an int array, no extra config.

When you need more control (snake_case, pretty printing, custom serializers), `RestMock.json()` and `RestMock.xml()` return the live `ObjectMapper` and `XmlMapper`. Configure them once before your tests run:

```java
@BeforeAll
static void configureSerialization() {
    RestMock.json()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .registerModule(new MyCustomModule());

    RestMock.xml()
            .enable(SerializationFeature.INDENT_OUTPUT);
}
```

These mappers are shared for the life of the JVM, and `clean()` does not reset
them: configuration set by one test class is still in force for the next one in
the same run. Configure them the same way everywhere, or set them per class if
two classes need different rules.

If even that's not enough, pre-serialize the response yourself and use the string overload; `thenReturnJSON(String)` accepts whatever you give it.

---

## Server lifecycle

```java
RestMock.startServer();     // default: localhost:9080
RestMock.clean();           // reset routes
RestMock.stopServer();
```

Those static methods are a facade over one default mock, which
`RestMock.defaultMock()` hands back. Everything below applies equally to an
`HttpMock` you construct yourself; see [running test classes in
parallel](#running-test-classes-in-parallel) for when you would.

Pass `0` to let the OS pick a free port and read it back; useful when several
builds share a CI machine and would otherwise fight over 9080:

```java
RestMock.startServer(0);
String baseUrl = "http://localhost:" + RestMock.port();
```

`RestMock.port()` returns `-1` while the server is stopped. Starting an
already-running server on a *different* port fails instead of quietly leaving
you pointed at the old one.

You rarely need the port itself. `baseUrl()` and `url()` build the address for
you, whichever port you ended up on:

```java
RestMock.baseUrl();          // http://localhost:9080
RestMock.url("/users/42");   // http://localhost:9080/users/42
```

Calling either while the server is stopped fails on the spot, rather than handing
back a URL that connects to nothing.

---

## JUnit extension

If you don't want to manage the server lifecycle yourself, rest-mock provides a JUnit extension that takes care of it for you.

`RestMockExtension` starts the server once before your tests run, cleans all routes after each test so they don't leak into each other, and stops the server when the class is done. You just declare it and write your tests:

```java
class MyApiTest {

    @RegisterExtension
    static RestMockExtension server = new RestMockExtension();

    @Test
    void fetchesUser() throws Exception {
        RestMock.whenGet("/users/1").thenReturnJSON("{\"name\": \"Bob\"}");

        // your HTTP client call here
    }

    @Test
    void createsUser() throws Exception {
        RestMock.whenPost("/users").thenReturnText("created ${name}");

        // routes from fetchesUser are already gone,
        // no manual clean() needed
    }
}
```

If you need a different port:

```java
@RegisterExtension
static RestMockExtension server = new RestMockExtension(3000);
```

Or `new RestMockExtension(0)` for an OS-assigned one, read back with
`RestMock.port()`.

### Running test classes in parallel

The static `RestMock` methods drive one process-wide mock, so classes sharing it
have to run one at a time. Give a class its own `HttpMock` and that constraint
goes away:

```java
@Execution(ExecutionMode.CONCURRENT)
class PaymentsTest {

    static HttpMock mock = new HttpMock();

    @RegisterExtension
    static RestMockExtension server = new RestMockExtension(mock, 0);

    @Test
    void chargesACard() throws Exception {
        mock.whenPost("/charges").thenReturnJSON("{\"id\":1}");

        // point the system under test at mock.baseUrl()
    }
}
```

Each `HttpMock` owns its routes, its request log, and its port, so two such
classes share nothing and can run at the same time. Port `0` matters here: it
lets the OS hand each one a free port instead of making you allocate them.

Instances share only the Jackson mappers behind `RestMock.json()` and
`RestMock.xml()`, which stay global because Jackson configuration is.

No base class. No `@BeforeAll`. No forgotten `clean()` calls. The extension handles everything so your tests only contain what matters: the mock setup and the assertion.

If some tests share the same routes and you don't want them cleaned between each test, use `keepRoutes()`:

```java
@RegisterExtension
static RestMockExtension server = new RestMockExtension().keepRoutes();
```

Routes will persist for the entire test class. You can still call `RestMock.clean()` manually whenever you need to.

---

## Design principles

- Minimal API surface  
- Inline over configuration  
- No hidden magic  
- No feature creep  

If a feature adds complexity, it doesn’t get added.

---

## When NOT to use rest-mock

Use another tool if you need:

- complex matching rules (regex on headers, body matchers)  
- full API simulation (stateful conversations, proxying)  
- record and replay from live traffic  

This library is intentionally not that.

---

## Philosophy

> The best test tools disappear into the test.

rest-mock is built to stay out of your way.
