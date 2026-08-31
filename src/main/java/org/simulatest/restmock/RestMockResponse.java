package org.simulatest.restmock;

/**
 * Configures the response for a stubbed route.
 *
 * Returned by {@link RestMock#whenGet(String) whenGet}, {@link RestMock#whenPost(String) whenPost},
 * etc. Call exactly one {@code thenReturn*} to set the body and content type;
 * the returned {@link ResponseOptions} lets you adjust status, headers, and delay.
 * If you never chain a {@code thenReturn*}, the route responds 501 with an
 * explanatory message.
 *
 * The {@code Object} overloads of {@code thenReturnJSON} and {@code thenReturnXML}
 * serialize through Jackson; raw {@code String} overloads pass the body through
 * verbatim. The {@code FromResource} variants load from the classpath, the same
 * place {@code Thread.currentThread().getContextClassLoader().getResource(path)} looks.
 * A missing or unreadable file throws {@link java.io.UncheckedIOException}: it is
 * a mistake in the test, not something to catch.
 * Text resources are decoded as UTF-8 and have leading and trailing whitespace
 * stripped; binary resources are passed through byte-for-byte.
 *
 * Response bodies may reference query parameters, form/JSON/XML body fields, and
 * path captures using {@code ${name}} placeholders. For example, a route
 * registered as {@code /users/{id}} can return {@code {"id":"${id}"}}.
 * Nested body fields use dotted paths ({@code ${user.name}}) and array elements
 * use indexes ({@code ${items.0.sku}}); for XML the root element is not part of
 * the path.
 *
 * Request headers are addressed under a {@code header.} prefix
 * ({@code ${header.X-Tenant}}), never as a bare {@code ${X-Tenant}}. The bare
 * namespace holds what the stub author wrote; Host, User-Agent and Accept are
 * attached by the HTTP client, and letting them share the namespace meant a typo
 * could silently resolve to one of them instead of failing. Names are matched
 * case-insensitively, so {@code ${header.X-Tenant}} resolves the header however
 * the server happened to canonicalize its name.
 *
 * A stub URI must be a path. A query string, a missing leading slash, and an
 * unclosed brace in a template are all rejected by the {@code when*} call with
 * an {@link IllegalArgumentException}; each used to compile into a route that
 * silently matched nothing.
 *
 * If a name appears in more than one source the most specific wins, in this order:
 * path captures, then body fields, then query parameters. Headers sit in their
 * own namespace and cannot collide with any of them. A placeholder with no
 * matching name fails the response with a 500 naming what was available, rather
 * than shipping {@code ${nmae}} to the client.
 *
 * Substituted values are escaped for the response format - JSON string escaping,
 * XML and HTML entities, nothing for plain text - so a request value carrying a
 * quote or an angle bracket cannot produce a malformed document. The
 * {@code thenReturnFile} methods skip substitution entirely.
 */
public interface RestMockResponse {

	/** Serializes {@code object} to JSON via {@link RestMock#json()}. Throws {@link java.io.UncheckedIOException} if Jackson cannot serialize. */
	ResponseOptions thenReturnJSON(Object object);

	/** Returns {@code json} as the response body, untouched. */
	ResponseOptions thenReturnJSON(String json);

	/** Loads a JSON file from the test classpath. */
	ResponseOptions thenReturnJSONFromResource(String path);

	/** Serializes {@code object} to XML via {@link RestMock#xml()}. Throws {@link java.io.UncheckedIOException} if Jackson cannot serialize. */
	ResponseOptions thenReturnXML(Object object);

	/** Returns {@code xml} as the response body, untouched. */
	ResponseOptions thenReturnXML(String xml);

	/** Loads an XML file from the test classpath. */
	ResponseOptions thenReturnXMLFromResource(String path);

	/** Returns {@code html} with content type {@code text/html}. */
	ResponseOptions thenReturnHTML(String html);

	/** Loads an HTML file from the test classpath. */
	ResponseOptions thenReturnHTMLFromResource(String path);

	/** Returns {@code txt} with content type {@code text/plain}. */
	ResponseOptions thenReturnText(String txt);

	/** Loads a text file from the test classpath. */
	ResponseOptions thenReturnTextFromResource(String path);

	/** Returns {@code bytes} as {@code application/octet-stream}. Bytes are sent verbatim, no parameter substitution. */
	ResponseOptions thenReturnFile(byte[] bytes);

	/** Returns {@code bytes} with the given content type. */
	ResponseOptions thenReturnFile(byte[] bytes, String contentType);

	/** Loads a file from the test classpath. Content type is guessed from the path extension. */
	ResponseOptions thenReturnFileFromResource(String path);

	/** Loads a file from the test classpath with an explicit content type. */
	ResponseOptions thenReturnFileFromResource(String path, String contentType);

	/** Returns {@code message} with the given HTTP status code as a plain-text body. */
	ResponseOptions thenReturnErrorCodeWithMessage(int errorCode, String message);

}
