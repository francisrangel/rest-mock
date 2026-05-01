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

If you need request verification or traffic inspection → use something else.

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

- request verification  
- call counting  
- complex matching rules  
- full API simulation  

This library is intentionally not that.

---

## Philosophy

> The best test tools disappear into the test.

rest-mock is built to stay out of your way.
