package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Guards the boundary between the supported API and the implementation.
 * Nothing under internal.* belongs in a public signature: it is documented as
 * free to change, and a caller who can name it is a caller we would break.
 */
public class PublicSurfaceTest {

	private static final List<Class<?>> PUBLIC_API = List.of(
		RestMock.class, HttpMock.class, RestMockExtension.class, RestMockResponse.class,
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
		for (Constructor<?> constructor : ResponseOptions.class.getDeclaredConstructors())
			assertFalse(Modifier.isPublic(constructor.getModifiers()),
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

	/**
	 * Every public type in the API package is one this test knows about, so a
	 * new public class cannot appear without being added here and checked.
	 */
	@Test
	public void everyPublicTypeInTheApiPackageIsChecked() throws IOException {
		Set<String> checked = PUBLIC_API.stream().map(Class::getSimpleName).collect(Collectors.toSet());
		Pattern declaration = Pattern.compile("^public (?:final |abstract )?(?:class|interface|record|enum) (\\w+)", Pattern.MULTILINE);

		try (Stream<Path> sources = Files.list(Path.of("src/main/java/org/simulatest/restmock"))) {
			for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
				Matcher declared = declaration.matcher(Files.readString(source));

				while (declared.find())
					assertTrue(checked.contains(declared.group(1)),
						declared.group(1) + " is public in the API package but PublicSurfaceTest does not check it");
			}
		}
	}

}
