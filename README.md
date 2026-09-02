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

That's it.

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
        assertEquals("John", users.byId(43).name());
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

Most HTTP mocking libraries start simple and turn into frameworks: dozens of
config options, verbose DSLs, JSON files everywhere. If you have used WireMock
or MockServer, you know the deal.

rest-mock goes the opposite direction. Mocking an endpoint should feel like
writing a return statement:

```java
RestMock.whenGet("/users/{id}")
        .thenReturnJSON("{\"id\":\"${id}\"}");
```

```
GET /users/42 → {"id":"42"}
```

No matchers. No request builders. No jumping between files.

**Zero setup.** It runs on the HTTP server already in the JDK. No Jetty, no
Netty, no containers; nothing to start but the mock itself.

**One mental model.** Anything the request carried is `${name}`: a path capture,
a query param, a body field. Request headers are the one exception, under a
`header.` prefix, because `Host` and `Accept` are things your client attached
rather than things you wrote.

---

## What you get

Everything you actually need, nothing you don't:

- All HTTP verbs: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS  
- HEAD and OPTIONS answered from the routes you already stubbed  
- Path templates: `/users/{id}`  
- Dynamic responses: any path capture, query param or body field by name, as `${name}`  
- JSON, XML, HTML, text  
- Load responses from files  
- Custom status codes and headers  
- Response delays for timeout testing  
- Sequenced responses for retry testing  
- A callback for the response that depends on the request  
- Request inspection and counting  
- JUnit extension for automatic lifecycle  
- Built-in CORS support, preflight included  
- 404s that tell you which stub you missed  
- One mock per test class, so classes can [run in parallel](#running-test-classes-in-parallel)  

And that's it.

---

## Reference

Everything above is the whole library in five minutes. What follows is the
detail, in the order you are likely to need it.

---

## Dynamic data from requests

Anything the request carried can be substituted into the response by name:

```java
RestMock.whenGet("/users/{id}")
        .thenReturnText("user ${id} aka ${nickname}");
```

```
GET /users/42?nickname=bob → user 42 aka bob
```

Path captures, query params and form, JSON or XML body fields all resolve the
same way. Nested fields use dotted paths, array elements use indexes:

```java
RestMock.whenPost("/orders")
        .thenReturnText("first sku: ${items.0.sku} for ${customer.name}");
```

Request headers are the exception, addressed under a `header.` prefix:

```java
RestMock.whenGet("/whoami").thenReturnText("tenant=${header.X-Tenant}");
```

The bare namespace holds what you wrote; `Host` and `Accept` were attached by
your client, and keeping them out means a typo can't quietly resolve to one
instead of failing. Header names are matched case-insensitively, so
`${header.X-Tenant}` finds the header however the server canonicalized it.
Every other name is matched exactly, the way `queryParam()` is: `${Name}` does
not find `name`.

When a name exists in more than one place the most specific wins: path captures,
then body fields, then query params. Headers cannot collide with any of them.

A placeholder that matches nothing is a mistake in the stub, so it fails rather
than shipping `${nmae}` to your client and letting a status-code assertion pass
anyway:

```
500  No value for ${nmae}. Available names: id, nickname; plus 6 request headers as ${header.NAME}
```

The names you wrote are listed and the headers are only counted, so the one you
were looking for is not buried under ten of them.

Substituted values are escaped for the format they land in, so a request cannot
break the document:

```java
RestMock.whenGet("/users/{id}").thenReturnJSON("{\"id\":\"${id}\"}");
```

```
GET /users/a"b → {"id":"a\"b"}
```

JSON gets string escaping, XML and HTML get entities, plain text is left alone.
`thenReturnFile` skips substitution entirely.

A body that has to contain a literal `${...}` doubles the dollar: `$${user}`
is served as `${user}`, untouched.

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
No stub for GET /orders

/orders is stubbed for POST, not GET.

Stubbed routes:
  GET     /users/1
  POST    /orders
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

When a response is too big to inline, put it in `src/test/resources` and pass the
filename. rest-mock loads it from the classpath and serves it with the matching
content type:

```java
RestMock.whenGet("/invoice").thenReturnJSONFromResource("invoice.json");
RestMock.whenGet("/report").thenReturnXMLFromResource("report.xml");
RestMock.whenGet("/page").thenReturnHTMLFromResource("page.html");
RestMock.whenGet("/readme").thenReturnTextFromResource("readme.txt");
```

Text resources are decoded as UTF-8 and trimmed, and `${...}` placeholders still
resolve. A missing file fails on the spot naming what it looked for. Nothing here
throws a checked exception, so stubbing never forces a `try`/`catch` onto a test.

---

## Serving files (images, PDFs, binaries)

`thenReturnFile` serves bytes as-is: no template substitution, no UTF-8 round
trip. The MIME type is inferred from the extension (`.png`, `.pdf`, `.zip` and
friends), falling back to `application/octet-stream`:

```java
RestMock.whenGet("/logo").thenReturnFileFromResource("logo.png");
RestMock.whenGet("/data").thenReturnFileFromResource("payload.bin", "application/x-protobuf");
```

When the bytes come from somewhere other than a classpath file, pass them inline.
Without a content type they default to `application/octet-stream`, since raw
bytes carry no filename to infer from:

```java
RestMock.whenGet("/invoice").thenReturnFile(generatePdf(invoice), "application/pdf");
```

These methods skip `${...}` substitution, which needs a string view of the
content. For text with placeholders, use the `*FromResource` methods above.

---

## Custom status codes and headers

Every response is 200 unless you say otherwise. `withStatus()` and `withHeader()`
chain onto any `thenReturn*`, in any combination:

```java
RestMock.whenPost("/users")
        .thenReturnJSON("{\"error\":\"email already taken\"}")
        .withStatus(422)
        .withHeader("Cache-Control", "no-cache")
        .withHeader("X-Request-Id", "abc123");
```

A header you set here wins over the one rest-mock would have sent, `Content-Type`
included.

When you just want a status and a message and don't care about the content type,
there's a shorthand:

```java
RestMock.whenGet("/secret")
        .thenReturnErrorCodeWithMessage(403, "Forbidden");
```

---

## Simulating slow responses

Testing timeouts, retries, or loading states? `withDelay()` makes a route wait
before responding:

```java
RestMock.whenGet("/slow-api")
        .thenReturnJSON("{\"data\":\"here\"}")
        .withDelay(2000);
```

Milliseconds are easy to misread, so there is an overload that says the unit out
loud:

```java
RestMock.whenGet("/slow-api")
        .thenReturnJSON("{\"data\":\"here\"}")
        .withDelay(Duration.ofSeconds(2));
```

The delay applies to that route only: other routes are served concurrently and
are not held up behind it. Routes without `withDelay()` respond immediately, and
a negative delay is rejected rather than ignored.

---

## Simulating an upstream that failed once

Testing a retry? Chain a second `thenReturn*` and the route serves its responses
in order, repeating the last one:

```java
RestMock.whenGet("/flaky")
        .thenReturnErrorCodeWithMessage(503, "down")
        .thenReturnText("up");
```

```
GET /flaky → 503 down
GET /flaky → 200 up
GET /flaky → 200 up
```

`withStatus()`, `withHeader()` and `withDelay()` apply to the response they
follow. Stubbing the route again with a new `when*` starts over.

---

## When the answer depends on the request

Placeholders cover most dynamic responses. When the status or the shape of the
body has to change with the request, `thenAnswer()` hands you the request and a
builder with the same `thenReturn*` methods, and serves whatever you build:

```java
RestMock.whenPost("/orders").thenAnswer((request, respond) -> {
    if (request.body().contains("sku")) respond.thenReturnJSON("{\"id\":1}").withStatus(201);
    else respond.thenReturnText("no sku").withStatus(400);
});
```

```
POST /orders {"sku":"A1"} → 201 {"id":1}
POST /orders {}           → 400 no sku
```

What you build is an ordinary response, so `${name}` placeholders still resolve.
A callback that throws answers 500 with the exception's message, and one that
builds nothing gets the same 501 as a stub with no `thenReturn*`.

---

## Inspecting received requests

Every request the server receives is recorded. Read them back through
`RestMock.requests()` to assert what your code actually called:

```java
RestMock.requests().countForPath("/api/users");            // how many hit it
RestMock.requests().forRoute(HttpMethod.POST, "/api/users"); // filter both ways
RestMock.requests().forPath("/health").isEmpty();          // did anything at all

ReceivedRequest last = RestMock.requests().lastForPath("/api/users").orElseThrow();

last.body();
last.header("Content-Type");   // Optional<String>, case-insensitive
last.queryParam("dry_run");    // Optional<String>, URL-decoded
```

The full set is `all()`, `forPath()`, `forMethod()`, `forRoute()`,
`countForPath()`, `countForRoute()`, `last()`, `lastForPath()` and `isEmpty()`.
For anything more specific, `all()` hands back the raw list to filter yourself.
Each `ReceivedRequest` carries the method, path, query string, headers, body and
timestamp.

Header names are case-insensitive, as HTTP itself is: `header()`,
`headerValues()` and the `headers()` map all find `Content-Type` however the JDK
server chose to spell it. Query parameter names stay case-sensitive.

When an assertion about the log fails, `expected: <1> but was: <0>` says nothing
about the calls that were actually made. Pass the log itself as the message and
it prints them:

```java
assertEquals(1, RestMock.requests().countForPath("/orders"), RestMock.requests()::toString);
```

```
org.opentest4j.AssertionFailedError: 2 requests received:
  1. GET     /users/1
  2. POST    /order (13 chars)
==> expected: <1> but was: <0>
```

The log is cleared by `RestMock.clean()` and by `RestMockExtension` between tests.

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
OPTIONS /users/1 → 204, Allow: GET, DELETE, HEAD, OPTIONS
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
  Access-Control-Allow-Methods: GET, HEAD, OPTIONS
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

Jackson does the serializing, so records, POJOs and getters just work, and any
module on your test classpath is picked up automatically:

```java
record Customer(String name, int age) {}

RestMock.whenGet("/me").thenReturnJSON(new Customer("Bob", 25));
```

When you need snake_case, pretty printing or a custom serializer,
`RestMock.json()` and `RestMock.xml()` return the live mappers to configure in a
`@BeforeAll`. They are shared for the life of the JVM and `clean()` does not
reset them, so configure them the same way everywhere or set them per class; the
javadoc on `RestMock.json()` spells the lifetime out.

Or skip the mapper entirely and pass a string: `thenReturnJSON(String)` serves
whatever you hand it, verbatim.

---

## Server lifecycle

```java
RestMock.startServer();     // default: localhost:9080
RestMock.clean();           // reset routes and the request log
RestMock.stopServer();
```

These static methods are a facade over one default mock, which
`RestMock.defaultMock()` hands back; everything here applies equally to an
`HttpMock` you construct yourself. See [running test classes in
parallel](#running-test-classes-in-parallel) for when you would.

Pass `0` to let the OS pick a free port, which is how builds sharing a CI machine
avoid fighting over 9080. You rarely need the number itself, because `baseUrl()`
and `url()` build the address whichever port you landed on:

```java
RestMock.startServer(0);

RestMock.baseUrl();          // http://localhost:54321
RestMock.url("/users/42");   // http://localhost:54321/users/42
```

`port()` returns `-1` while stopped, and both `baseUrl()` and `url()` fail on the
spot rather than handing back a URL that connects to nothing. Starting an
already-running server on a *different* port fails too, instead of quietly
leaving you pointed at the old one.

---

## JUnit extension

`RestMockExtension` starts the server before the class, clears routes after each
test so they cannot leak into the next, and stops it when the class is done. No
base class, no `@BeforeAll`, no forgotten `clean()`:

```java
class MyApiTest {

    @RegisterExtension
    static RestMockExtension server = new RestMockExtension();

    @Test
    void fetchesUser() throws Exception {
        RestMock.whenGet("/users/1").thenReturnJSON("{\"name\":\"Bob\"}");

        // point your client at RestMock.baseUrl()
    }
}
```

Register it on a `static` field: a non-static one is rebuilt for every test, and
you would get a server per test instead of per class.

The port is assigned by the OS, so two builds sharing a CI machine never fight
over one; `RestMock.baseUrl()` is the address either way. `new
RestMockExtension(3000)` pins a port when something outside the test has to
know it. `keepRoutes()` turns off the per-test reset when a class shares one
fixture:

```java
@RegisterExtension
static RestMockExtension server = new RestMockExtension().keepRoutes();
```

---

## Running test classes in parallel

The static `RestMock` methods drive one process-wide mock, so classes sharing it
have to run one at a time. Give a class its own `HttpMock` and that constraint
goes away:

```java
@Execution(ExecutionMode.CONCURRENT)
class PaymentsTest {

    static HttpMock mock = new HttpMock();

    @RegisterExtension
    static RestMockExtension server = new RestMockExtension(mock);

    @Test
    void chargesACard() throws Exception {
        mock.whenPost("/charges").thenReturnJSON("{\"id\":1}");

        // point the system under test at mock.baseUrl()
    }
}
```

Each `HttpMock` owns its routes, its request log, and its port, so two such
classes share nothing and can run at the same time. Each binds its own
OS-assigned port, so nothing has to allocate them.

`@Execution` only does anything once JUnit's parallel support is switched on, in
`src/test/resources/junit-platform.properties`:

```properties
junit.jupiter.execution.parallel.enabled = true
junit.jupiter.execution.parallel.mode.default = same_thread
junit.jupiter.execution.parallel.mode.classes.default = same_thread
```

Leaving both defaults at `same_thread` means nothing runs concurrently unless it
asks to. Classes still using the static `RestMock` API keep running one at a
time, and only the ones you annotate opt in.

Instances share only the Jackson mappers behind `RestMock.json()` and
`RestMock.xml()`, which stay global because Jackson configuration is.

---

## Design principles

- Minimal API surface  
- Inline over configuration  
- No hidden magic  
- No feature creep  

If a feature adds complexity, it doesn't get added.

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
