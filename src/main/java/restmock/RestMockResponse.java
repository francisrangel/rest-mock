package restmock;

import java.io.IOException;

import restmock.response.ResponseOptions;

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
 * Text resources are decoded as UTF-8 and have leading and trailing whitespace
 * stripped; binary resources are passed through byte-for-byte.
 *
 * Response bodies may reference query parameters, headers, JSON/XML body fields,
 * and path captures using {@code ${name}} placeholders. For example, a route
 * registered as {@code /users/{id}} can return {@code {"id":"${id}"}}. If a name
 * appears in more than one source, path captures override query, header, and
 * body fields (in that order).
 */
public interface RestMockResponse {

	/** Serializes {@code object} to JSON via {@link RestMock#json()}. Throws {@link java.io.UncheckedIOException} if Jackson cannot serialize. */
	ResponseOptions thenReturnJSON(Object object);

	/** Returns {@code json} as the response body, untouched. */
	ResponseOptions thenReturnJSON(String json);

	/** Loads a JSON file from the test classpath. */
	ResponseOptions thenReturnJSONFromResource(String path) throws IOException;

	/** Serializes {@code object} to XML via {@link RestMock#xml()}. Throws {@link java.io.UncheckedIOException} if Jackson cannot serialize. */
	ResponseOptions thenReturnXML(Object object);

	/** Returns {@code xml} as the response body, untouched. */
	ResponseOptions thenReturnXML(String xml);

	/** Loads an XML file from the test classpath. */
	ResponseOptions thenReturnXMLFromResource(String path) throws IOException;

	/** Returns {@code html} with content type {@code text/html}. */
	ResponseOptions thenReturnHTML(String html);

	/** Loads an HTML file from the test classpath. */
	ResponseOptions thenReturnHTMLFromResource(String path) throws IOException;

	/** Returns {@code txt} with content type {@code text/plain}. */
	ResponseOptions thenReturnText(String txt);

	/** Loads a text file from the test classpath. */
	ResponseOptions thenReturnTextFromResource(String path) throws IOException;

	/** Returns {@code bytes} as {@code application/octet-stream}. Bytes are sent verbatim, no parameter substitution. */
	ResponseOptions thenReturnFile(byte[] bytes);

	/** Returns {@code bytes} with the given content type. */
	ResponseOptions thenReturnFile(byte[] bytes, String contentType);

	/** Loads a file from the test classpath. Content type is guessed from the path extension. */
	ResponseOptions thenReturnFileFromResource(String path) throws IOException;

	/** Loads a file from the test classpath with an explicit content type. */
	ResponseOptions thenReturnFileFromResource(String path, String contentType) throws IOException;

	/** Returns {@code message} with the given HTTP status code as a plain-text body. */
	ResponseOptions thenReturnErrorCodeWithMessage(int errorCode, String message);

}
