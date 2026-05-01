# rest-mock

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
- Path templates: `/users/{id}`  
- Dynamic responses: `${id}`, `${query}`, `${body}`  
- JSON, XML, HTML, text  
- Load responses from files  
- Custom status codes and headers  
- Response delays for timeout testing  
- Request inspection and counting  
- JUnit extension for automatic lifecycle  
- Built-in CORS support  

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

## Quick example

```java
record Person(String name) {}

RestMock.whenGet("/users/42")
        .thenReturnJSON("{\"name\":\"Bob\"}");

RestMock.whenGet("/users/43")
        .thenReturnJSON(new Person("John"));

RestMock.startServer();
```

```
GET /users/42 → {"name":"Bob"}
GET /users/43 → {"name":"John"}
```

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

Delay chains with `withStatus()` and `withHeader()` like everything else. Routes without `withDelay()` respond immediately.

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

// Filter by method and path
List<ReceivedRequest> posts = RestMock.requests()
        .forRoute(HttpMethod.POST, "/api/users");

// Did anything hit this endpoint?
RestMock.requests().forPath("/health").isEmpty();
```

Each `ReceivedRequest` captures the method, path, query string, headers, body, and timestamp. The `RequestLog` provides common filters out of the box:

- `all()` — every request in arrival order
- `forPath(path)` — literal path match
- `forMethod(method)` — filter by HTTP verb
- `forRoute(method, path)` — both at once
- `countForPath(path)`, `countForRoute(method, path)` — counts
- `last()`, `lastForPath(path)` — most recent
- `isEmpty()` — quick check

For anything more specific, `all()` gives you the raw list to filter however you want.

The request log is cleared automatically when you call `RestMock.clean()` or when the `RestMockExtension` cleans between tests.

---

## Install

```xml
<dependency>
  <groupId>org</groupId>
  <artifactId>restmock</artifactId>
  <version>0.0.1</version>
  <scope>test</scope>
</dependency>
```

---

## Server lifecycle

```java
RestMock.startServer();     // default: localhost:9080
RestMock.clean();           // reset routes
RestMock.stopServer();
```

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
