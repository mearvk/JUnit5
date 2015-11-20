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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.gen5.api.Assertions.assertEquals;
import static org.junit.gen5.api.Assertions.assertNotEquals;
import static org.junit.gen5.api.Assertions.assertNotNull;
import static org.junit.gen5.api.Assertions.assertNotSame;
import static org.junit.gen5.api.Assertions.assertNull;
import static org.junit.gen5.api.Assertions.assertSame;
import static org.junit.gen5.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Objects;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opentestalliance.ComparisonError;
import org.opentestalliance.IdentityAssertionError;
import org.opentestalliance.NotEqualAssertionError;
import org.opentestalliance.NotIdenticalAssertionError;
import org.opentestalliance.NotNullAssertionError;
import org.opentestalliance.NullComparisonError;
import org.opentestalliance.TestAssertionError;

/**
 * Unit tests for {@link Assertions}.
 *
 * @author Sam Brannen
 * @since 5.0
 */
public class AssertionsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void assertNullWithNull() {
		assertNull(null);
	}

	@Test
	public void assertNullWithNonNullObject() {
		exception.expect(NullComparisonError.class);
		exception.expectMessage(endsWith("expected:<null>, actual:<foo>"));
		assertNull("foo");
	}

	@Test
	public void assertNotNullWithNonNullObject() {
		assertNotNull("foo");
	}

	@Test
	public void assertNotNullWithNull() {
		exception.expect(NotNullAssertionError.class);
		exception.expectMessage(startsWith("Person"));
		exception.expectMessage(endsWith("expected:<non-null object>, actual:<null>"));
		assertNotNull(null, "Person");
	}

	@Test
	public void assertEqualsWithNullObjects() {
		assertEquals(null, null);
	}

	@Test
	public void assertEqualsWithSameObject() {
		String foo = "bar";
		assertEquals(foo, foo);
	}

	@Test
	public void assertEqualsWithEqualObjects() {
		Person person1 = new Person("Homer");
		Person person2 = new Person("Homer");
		assertEquals(person1, person2);
	}

	@Test
	public void assertEqualsWithOneNullExpectedObject() {
		exception.expect(ComparisonError.class);
		exception.expectMessage(endsWith("expected:<foo>, actual:<null>"));
		assertEquals("foo", null);
	}

	@Test
	public void assertEqualsWithOneNullActualObject() {
		exception.expect(ComparisonError.class);
		exception.expectMessage(endsWith("expected:<null>, actual:<foo>"));
		assertEquals(null, "foo");
	}

	@Test
	public void assertNotEqualsWithDifferentObjects() {
		assertNotEquals("foo", "bar");
	}

	@Test
	public void assertNotEqualsWithNullFirstObject() {
		assertNotEquals(null, "bar");
	}

	@Test
	public void assertNotEqualsWithNullSecondObject() {
		assertNotEquals("foo", null);
	}

	@Test
	public void assertNotEqualsWithSameObject() {
		String foo = "bar";
		exception.expect(NotEqualAssertionError.class);
		exception.expectMessage(endsWith("expected:<unequal objects>, actual:<bar>"));
		assertNotEquals(foo, foo);
	}

	@Test
	public void assertNotEqualsWithEqualObjects() {
		exception.expect(NotEqualAssertionError.class);
		exception.expectMessage(endsWith("expected:<unequal objects>, actual:<Homer>"));
		assertNotEquals(new Person("Homer"), new Person("Homer"));
	}

	@Test
	public void assertSameWithNullObjects() {
		assertSame(null, null);
	}

	@Test
	public void assertSameWithAutoboxing() {
		assertSame(1, 1);
	}

	@Test
	public void assertSameWithSameObject() {
		String foo = "bar";
		assertSame(foo, foo);
	}

	@Test
	public void assertSameWithOneNullFirstObject() {
		exception.expect(IdentityAssertionError.class);
		exception.expectMessage(endsWith("expected:<null>, actual:<Homer>"));
		assertSame(null, new Person("Homer"));
	}

	@Test
	public void assertSameWithOneNullSecondObject() {
		exception.expect(IdentityAssertionError.class);
		exception.expectMessage(endsWith("expected:<Homer>, actual:<null>"));
		assertSame(new Person("Homer"), null);
	}

	@Test
	public void assertSameWithEqualObjects() {
		exception.expect(IdentityAssertionError.class);
		exception.expectMessage(containsString("expected:<" + Person.class.getName()));
		exception.expectMessage(containsString("(Homer)"));
		exception.expectMessage(containsString("actual:<" + Person.class.getName()));
		assertSame(new Person("Homer"), new Person("Homer"));
	}

	@Test
	public void assertSameWithDifferentObjectsWithSameToString() {
		exception.expect(IdentityAssertionError.class);
		exception.expectMessage(containsString("expected:<" + Person.class.getName()));
		exception.expectMessage(containsString("(Homer)"));
		exception.expectMessage(containsString("actual:<" + String.class.getName()));
		assertSame(new Person("Homer"), "Homer");
	}

	@Test
	public void assertNotSameWithDifferentObjects() {
		assertNotSame("foo", "bar");
	}

	@Test
	public void assertNotSameWithNullFirstObject() {
		assertNotSame(null, "bar");
	}

	@Test
	public void assertNotSameWithNullSecondObject() {
		assertNotSame("foo", null);
	}

	@Test
	public void assertNotSameWithSameObject() {
		String foo = "bar";
		exception.expect(NotIdenticalAssertionError.class);
		exception.expectMessage(containsString("expected:<different objects>"));
		exception.expectMessage(containsString("actual:<" + String.class.getName()));
		assertNotSame(foo, foo);
	}

	@Test
	public void assertNotSameWithNullObjects() {
		exception.expect(NotIdenticalAssertionError.class);
		exception.expectMessage(endsWith("expected:<different objects>, actual:<null>"));
		assertNotSame(null, null);
	}

	@Test
	public void assertThrowsWithExpectedExceptionThrown() {
		assertThrows(IndexOutOfBoundsException.class, () -> {
			Arrays.asList("foo").get(1);
		});
	}

	@Test
	public void assertThrowsWithUnexpectedExceptionThrown() {
		exception.expect(ComparisonError.class);
		exception.expectMessage(startsWith("Unexpected exception type thrown"));
		exception.expectMessage(containsString("expected:<java.lang.NullPointerException>"));
		exception.expectMessage(containsString("actual:<java.lang.ArrayIndexOutOfBoundsException>"));

		assertThrows(NullPointerException.class, () -> {
			Arrays.asList("foo").get(1);
		});
	}

	@Test
	public void assertThrowsWithoutExpectedExceptionThrown() {
		exception.expect(TestAssertionError.class);
		exception.expectMessage(
			equalTo(String.format("Expected exception of type %s to be thrown, but nothing was thrown",
				IndexOutOfBoundsException.class.getName())));

		assertThrows(IndexOutOfBoundsException.class, () -> {
			/* no-op */
		});
	}

	// -------------------------------------------------------------------

	private static class Person {

		private final String name;

		Person(String name) {
			this.name = name;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Person other = (Person) obj;
			return Objects.equals(this.name, other.name);
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

}
