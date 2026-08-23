package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Guards the boundary between the supported API and the implementation.
 * Nothing under internal.* belongs in a public signature: it is documented as
 * free to change, and a caller who can name it is a caller we would break.
 */
public class PublicSurfaceTest {

	private static final List<Class<?>> PUBLIC_API = List.of(
		RestMock.class, RestMockExtension.class, RestMockResponse.class,
		ResponseOptions.class, RequestLog.class, ReceivedRequest.class, HttpMethod.class);

	@Test
	public void noPublicMemberMentionsAnInternalType() {
		for (Class<?> type : PUBLIC_API) {
			for (Method method : type.getDeclaredMethods()) {
				if (!Modifier.isPublic(method.getModifiers())) continue;

				assertNoInternalTypes(type.getSimpleName() + "." + method.getName(), method.getReturnType());
				assertNoInternalTypes(type.getSimpleName() + "." + method.getName(), method.getParameterTypes());
			}

			for (Constructor<?> constructor : type.getDeclaredConstructors()) {
				if (!Modifier.isPublic(constructor.getModifiers())) continue;

				assertNoInternalTypes(type.getSimpleName() + " constructor", constructor.getParameterTypes());
			}
		}
	}

	@Test
	public void responseOptionsCannotBeConstructedByCallers() {
		assertEquals(1, ResponseOptions.class.getDeclaredConstructors().length);
		assertFalse(Modifier.isPublic(ResponseOptions.class.getDeclaredConstructors()[0].getModifiers()),
			"ResponseOptions used to expose a public constructor taking an internal Response");
	}

	private static void assertNoInternalTypes(String member, Class<?>... types) {
		for (Class<?> type : types) {
			Class<?> element = type.isArray() ? type.getComponentType() : type;

			assertFalse(element.getName().startsWith("org.simulatest.restmock.internal"),
				member + " exposes the internal type " + element.getName()
					+ "; internal.* is documented as unstable, so it must not appear in a public signature");
		}
	}

	@Test
	public void thePublicApiIsTheDocumentedSetOfTypes() {
		List<String> names = PUBLIC_API.stream().map(Class::getSimpleName).sorted().toList();

		assertEquals(
			Arrays.asList("HttpMethod", "ReceivedRequest", "RequestLog", "ResponseOptions",
				"RestMock", "RestMockExtension", "RestMockResponse"),
			names);
	}

}
