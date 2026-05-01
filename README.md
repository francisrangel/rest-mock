# rest-mock

A tiny library for stubbing HTTP endpoints in JVM tests.

One static call sets up a route. Another starts the server. That's the whole API.

```java
record Person(String name) {}

RestMock.whenGet("/users/42").thenReturnJSON("{\"name\":\"Bob\"}");
RestMock.whenGet("/users/43").thenReturnJSON(new Person("John"));
RestMock.startServer();
// GET http://localhost:9080/users/42  →  {"name":"Bob"}
// GET http://localhost:9080/users/43  →  {"name":"John"}
```

No config files, no annotations, nothing else to learn. Drop it in, mock the endpoint, write the test.

---

## Why rest-mock

Every Java HTTP-mock library starts simple and grows into a configuration framework. rest-mock is the opposite bet: keep the surface area small enough that a junior engineer reads two examples and is productive, and refuse to add features that require new concepts to learn.

What you get:

- **All seven standard HTTP verbs:** GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS.
- **Path templates:** `/users/{id}` matches `/users/42` and exposes `${id}`.
- **Dynamic responses:** values from the request URL, query string, form body, or JSON body flow into `${...}` placeholders in the response.
- **Multiple content types:** JSON, XML, HTML, plain text, with automatic `Content-Type` headers.
- **Resource files:** load response bodies from `src/test/resources/`, or inline them if you really want.
- **Custom status codes and headers:** for testing error paths and header-driven behavior.
- **CORS headers out of the box:** useful when calling the mock from a browser-based test.
- **Tiny footprint.** One test-scope dependency (XStream, for object-to-JSON/XML serialization). The HTTP server is the JDK's built-in `com.sun.net.httpserver`, not a bundled Jetty or Netty.

## Requirements

- A JVM, version 17 or newer. Any JVM language works: Java, Kotlin, Scala, Groovy, Clojure, etc. The public API is plain static methods on `restmock.RestMock`, with no Java-specific features (no varargs, no SAM lambdas, no annotations to scan), so calls from any JVM language look the same.

## Install

Include `org:restmock:0.0.1` in your build. For Maven:

```xml
<dependency>
    <groupId>org</groupId>
    <artifactId>restmock</artifactId>
    <version>0.0.1</version>
    <scope>test</scope>
</dependency>
```

## Quickstart

```java
import restmock.RestMock;

@Before
public void setUp() {
    RestMock.startServer();          // listens on http://localhost:9080
}

@After
public void tearDown() {
    RestMock.clean();                // forgets registered routes
    RestMock.stopServer();
}

@Test
public void greetsByName() {
    RestMock.whenGet("/hello").thenReturnText("Hello ${name}!");

    // GET http://localhost:9080/hello?name=Bob  →  "Hello Bob!"
}
```

The default port is `9080`. To use a different one, call `RestMock.startServer(port)`.

The same calls work unchanged from Kotlin, Scala, or Groovy: `RestMock.whenGet("/hello").thenReturnText("hi")` is identical in all of them.

---

## HTTP methods

Each verb has a matching `whenX` factory. The chained call decides what comes back.

```java
RestMock.whenGet("/users").thenReturnJSON(allUsers);
RestMock.whenPost("/users").thenReturnText("created");
RestMock.whenPut("/users/{id}").thenReturnText("updated ${id}");
RestMock.whenDelete("/users/{id}").thenReturnText("deleted ${id}");
RestMock.whenPatch("/users/{id}").thenReturnText("patched ${id}");
RestMock.whenHead("/users/{id}").thenReturnText("ignored body");
RestMock.whenOptions("/users").thenReturnText("ok");
```

Notes on the less obvious verbs:

- **HEAD** sets `Content-Length` to the size the GET equivalent would have, and sends no body. RFC-compliant.
- **OPTIONS** automatically adds an `Allow` header listing every method registered for the same path, so a preflight request gets a useful answer without you wiring it up.

## Path templates

A `{name}` segment in a registered URI matches a single non-slash segment of the incoming request and exposes the captured value as `${name}` in the response.

```java
RestMock.whenGet("/users/{id}").thenReturnText("user ${id}");
// GET /users/42  →  "user 42"

RestMock.whenGet("/users/{userId}/posts/{postId}").thenReturnJSON(
    "{\"u\":\"${userId}\",\"p\":\"${postId}\"}");
// GET /users/7/posts/99  →  {"u":"7","p":"99"}
```

Templates do not span `/`. `/users/{id}` does **not** match `/users/1/extra`; that returns 404.

### Specificity

When more than one registered template can match the same incoming path, **the one with fewer placeholders wins**. Registration order does not matter:

```java
RestMock.whenGet("/users/{id}").thenReturnText("any user");
RestMock.whenGet("/users/me").thenReturnText("you");

// GET /users/me  →  "you"        (literal beats template)
// GET /users/42  →  "any user"   (only the template matches)
```

This is the only rule. There is no priority configuration, no matcher API, no route ordering.

## Dynamic responses

The same `${name}` substitution works for everything you can extract from the request. Sources are merged into one parameter map, in this order (later wins):

1. Path captures (`{name}` segments)
2. Query string parameters (`?name=Bob`)
3. Request body, if it is `application/x-www-form-urlencoded` or `application/json`

### Path + query

```java
RestMock.whenGet("/users/{id}").thenReturnText("user ${id} aka ${nickname}");
// GET /users/42?nickname=bob  →  "user 42 aka bob"
```

### Form-URL-encoded body

```java
RestMock.whenPost("/login").thenReturnText("hello ${username}");
// POST /login  with  username=Bob  →  "hello Bob"
```

### JSON body

JSON bodies are flattened into dotted keys. Nested objects use `.`, array indexes use the integer:

```java
RestMock.whenPost("/login").thenReturnText("hello ${user.name}");
// POST /login  Content-Type: application/json  body: {"user":{"name":"Bob"}}
//   →  "hello Bob"

RestMock.whenPost("/items").thenReturnText("first=${items.0.x}");
// body: {"items":[{"x":"a"},{"x":"b"}]}  →  "first=a"
```

A malformed JSON body is silently ignored: the route still responds, but `${...}` placeholders that depended on the body stay literal. This keeps tests resilient to typos in test fixtures rather than failing with parser errors.

## Content types

```java
RestMock.whenGet("/u").thenReturnJSON(new Developer("Bob", 25));
// →  {"name":"Bob","age":25}                  Content-Type: application/json

RestMock.whenGet("/u").thenReturnXML(new Developer("Bob", 25));
// →  <?xml version="1.0" ?><developer>...     Content-Type: text/xml

RestMock.whenGet("/u").thenReturnHTML("<h1>hi</h1>");          // text/html
RestMock.whenGet("/u").thenReturnText("hi");                   // text/plain
```

The object-form `thenReturnJSON(obj)` / `thenReturnXML(obj)` use XStream for serialization. The string-form passes the body through verbatim, handy when you already have a recorded fixture.

## Loading response bodies from files

Put fixture files under `src/test/resources/` and reference them by name.

```java
RestMock.whenGet("/big").thenReturnJSONFromResource("big-response.json");
RestMock.whenGet("/page").thenReturnHTMLFromResource("index.html");
RestMock.whenGet("/doc").thenReturnXMLFromResource("invoice.xml");
RestMock.whenGet("/blob").thenReturnTextFromResource("dump.txt");
```

`${...}` substitution still applies to the file contents.

## Status codes

```java
import java.net.HttpURLConnection;

RestMock.whenGet("/secret")
    .thenReturnErrorCodeWithMessage(HttpURLConnection.HTTP_FORBIDDEN, "no");
// →  HTTP/1.1 403 Forbidden     body: "no"
```

Any status code works. Use this for error-path tests.

## Custom headers

```java
RestMock.whenGet("/hello")
    .thenReturnText("hi")
    .withHeader("Cache-Control", "no-cache")
    .withHeader("X-Trace-Id", "abc123");
```

`withHeader` is chainable.

## Server lifecycle

```java
RestMock.startServer();           // default port 9080
RestMock.startServer(8081);       // or pick your own
RestMock.clean();                 // discard all registered routes
RestMock.stopServer();            // shut down
```

Typical JUnit pattern: start once per class (`@BeforeClass`), `clean()` between tests (`@After`), `stopServer()` once at the end (`@AfterClass`).

## Design philosophy

A few choices worth being explicit about:

- **One mental model for parameters.** Anything you sent, whether path segment, query value, form field, or JSON property, comes back as `${name}`. There is no per-source DSL to learn.
- **Specificity beats configuration.** Routes with fewer placeholders win. No priorities, no `setOrder(int)`.
- **Failure is silent and recoverable.** Malformed JSON, missing parameters, and unknown content types fall back to leaving placeholders literal rather than throwing. Tests that don't exercise a code path shouldn't break it.
- **No matchers, no expectations, no verifications.** rest-mock answers HTTP requests with the data you told it to. If you need "assert this endpoint was called N times", reach for a different tool, and let us know in an issue if you think we should reconsider.
