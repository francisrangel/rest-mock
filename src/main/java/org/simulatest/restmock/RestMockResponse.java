package org.simulatest.restmock;

import java.util.function.BiConsumer;

/**
 * Configures the response for a stubbed route.
 *
 * Returned by {@link RestMock#whenGet(String) whenGet}, {@link RestMock#whenPost(String) whenPost},
 * etc. Call a {@code thenReturn*} to set the body and content type; the
 * returned {@link ResponseOptions} lets you adjust status, headers, and delay,
 * and a further {@code thenReturn*} on it queues the next response for the
 * route. If you never chain a {@code thenReturn*}, the route responds 501 with
 * an explanatory message.
 *
 * The {@code Object} overloads of {@code thenReturnJSON} and {@code thenReturnXML}
 * serialize through Jackson; raw {@code String} overloads pass the body through
 * verbatim. The {@code FromResource} variants load from the test classpath, and
 * a missing file throws {@link java.io.UncheckedIOException}.
 *
 * A text body may reference what the request carried as {@code ${name}}: path
 * captures, query parameters, form, JSON or XML body fields, and request
 * headers as {@code ${header.Name}}. Values are escaped for the body's format,
 * a name with no value fails the response with a 500, and {@code thenReturnFile}
 * skips substitution. The
 * <a href="https://github.com/francisrangel/rest-mock#dynamic-data-from-requests">README</a>
 * spells out the rules.
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

	/**
	 * Decides the response per request. The callback receives the request and a
	 * builder with the same {@code thenReturn*} methods as this one; whatever it
	 * builds last is served. It is the one place a stub may branch on what the
	 * request carried:
	 *
	 *   {@code RestMock.whenPost("/orders").thenAnswer((request, respond) ->}
	 *   {@code     respond.thenReturnJSON("{\"id\":1}").withStatus(request.body().contains("sku") ? 201 : 400));}
	 *
	 * A callback that throws answers 500 with the exception's message, like an
	 * unresolved placeholder does.
	 */
	void thenAnswer(BiConsumer<ReceivedRequest, RestMockResponse> answer);

}
