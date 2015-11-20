/*
 * Copyright 2015 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.api;

import java.util.Objects;
import java.util.function.Supplier;

import org.opentestalliance.ComparisonError;
import org.opentestalliance.IdentityAssertionError;
import org.opentestalliance.MultipleFailuresError;
import org.opentestalliance.NotEqualAssertionError;
import org.opentestalliance.NotIdenticalAssertionError;
import org.opentestalliance.NotNullAssertionError;
import org.opentestalliance.NullComparisonError;
import org.opentestalliance.TestAssertionError;

/**
 * @author JUnit Community
 * @author Sam Brannen
 * @since 5.0
 */
public final class Assertions {

	private Assertions() {
		/* no-op */
	}

	public static void fail(String message) {
		if (message == null) {
			throw new TestAssertionError();
		}
		throw new TestAssertionError(message);
	}

	public static void fail(Supplier<String> messageSupplier) {
		fail(nullSafeGet(messageSupplier));
	}

	public static void assertTrue(boolean condition) {
		assertTrue(condition, (String) null);
	}

	public static void assertTrue(boolean condition, String message) {
		if (!condition) {
			fail(message);
		}
	}

	public static void assertTrue(boolean condition, Supplier<String> messageSupplier) {
		if (!condition) {
			fail(messageSupplier);
		}
	}

	public static void assertFalse(boolean condition) {
		assertFalse(condition, (String) null);
	}

	public static void assertFalse(boolean condition, String message) {
		if (condition) {
			fail(message);
		}
	}

	public static void assertFalse(boolean condition, Supplier<String> messageSupplier) {
		if (condition) {
			fail(messageSupplier);
		}
	}

	public static void assertNull(Object obj) {
		assertNull(obj, (String) null);
	}

	public static void assertNull(Object obj, String message) {
		if (obj != null) {
			throw new NullComparisonError(obj, message);
		}
	}

	public static void assertNull(Object obj, Supplier<String> messageSupplier) {
		if (obj != null) {
			throw new NullComparisonError(obj, nullSafeGet(messageSupplier));
		}
	}

	public static void assertNotNull(Object obj) {
		assertNotNull(obj, (String) null);
	}

	public static void assertNotNull(Object obj, String message) {
		if (obj == null) {
			throw new NotNullAssertionError(obj, message);
		}
	}

	public static void assertNotNull(Object obj, Supplier<String> messageSupplier) {
		if (obj == null) {
			throw new NotNullAssertionError(obj, nullSafeGet(messageSupplier));
		}
	}

	public static void assertEquals(Object expected, Object actual) {
		assertEquals(expected, actual, (String) null);
	}

	public static void assertEquals(Object expected, Object actual, String message) {
		if (!Objects.equals(expected, actual)) {
			throw new ComparisonError(expected, actual, message);
		}
	}

	public static void assertEquals(Object expected, Object actual, Supplier<String> messageSupplier) {
		if (!Objects.equals(expected, actual)) {
			throw new ComparisonError(expected, actual, nullSafeGet(messageSupplier));
		}
	}

	public static void assertNotEquals(Object expected, Object actual) {
		assertNotEquals(expected, actual, (String) null);
	}

	public static void assertNotEquals(Object expected, Object actual, String message) {
		if (Objects.equals(expected, actual)) {
			throw new NotEqualAssertionError(expected, message);
		}
	}

	public static void assertNotEquals(Object expected, Object actual, Supplier<String> messageSupplier) {
		if (Objects.equals(expected, actual)) {
			throw new NotEqualAssertionError(expected, nullSafeGet(messageSupplier));
		}
	}

	public static void assertSame(Object expected, Object actual) {
		assertSame(expected, actual, (String) null);
	}

	public static void assertSame(Object expected, Object actual, String message) {
		if (expected != actual) {
			throw new IdentityAssertionError(expected, actual, message);
		}
	}

	public static void assertSame(Object expected, Object actual, Supplier<String> messageSupplier) {
		if (expected != actual) {
			throw new IdentityAssertionError(expected, actual, nullSafeGet(messageSupplier));
		}
	}

	public static void assertNotSame(Object expected, Object actual) {
		assertNotSame(expected, actual, (String) null);
	}

	public static void assertNotSame(Object expected, Object actual, String message) {
		if (expected == actual) {
			throw new NotIdenticalAssertionError(expected, message);
		}
	}

	public static void assertNotSame(Object expected, Object actual, Supplier<String> messageSupplier) {
		if (expected == actual) {
			throw new NotIdenticalAssertionError(expected, nullSafeGet(messageSupplier));
		}
	}

	public static void assertAll(Executable... asserts) {
		assertAll("Multiple failures:", asserts);
	}

	public static void assertAll(String heading, Executable... asserts) {
		MultipleFailuresError multipleFailuresError = new MultipleFailuresError(heading);
		for (Executable executable : asserts) {
			try {
				executable.execute();
			}
			catch (AssertionError failure) {
				multipleFailuresError.addFailure(failure);
			}
			catch (RuntimeException | Error ex) {
				throw ex;
			}
			catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		}
		if (multipleFailuresError.hasFailures()) {
			throw multipleFailuresError;
		}
	}

	public static void assertThrows(Class<? extends Throwable> expected, Executable executable) {
		expectThrows(expected, executable);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Throwable> T expectThrows(Class<T> expected, Executable executable) {
		try {
			executable.execute();
		}
		catch (Throwable actual) {
			if (expected.isInstance(actual)) {
				return (T) actual;
			}
			throw new ComparisonError(expected.getName(), actual.getClass().getName(),
				"Unexpected exception type thrown");
		}
		throw new TestAssertionError(
			String.format("Expected exception of type %s to be thrown, but nothing was thrown", expected.getName()));
	}

	private static String nullSafeGet(Supplier<String> messageSupplier) {
		return (messageSupplier != null ? messageSupplier.get() : null);
	}

}
