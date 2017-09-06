/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.apiguardian.api.API.Status.STABLE;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;

/**
 * Mutable descriptor for a test or container that has been discovered by a
 * {@link TestEngine}.
 *
 * @see TestEngine
 * @since 1.0
 */
@API(status = STABLE)
public interface TestDescriptorMutable extends TestDescriptor {
	/**
	 * Set the <em>parent</em> of this descriptor.
	 *
	 * @param parent the new parent of this descriptor; may be {@code null}.
	 */
	void setParent(TestDescriptorMutable parent);

	/**
	 * Add a <em>child</em> to this descriptor.
	 *
	 * @param descriptor the child to add to this descriptor; never {@code null}
	 */
	void addChild(TestDescriptorMutable descriptor);

	@Override
	Set<? extends TestDescriptorMutable> getChildren();

	/**
	 * Remove a <em>child</em> from this descriptor.
	 *
	 * @param descriptor the child to remove from this descriptor; never
	 * {@code null}
	 */
	void removeChild(TestDescriptorMutable descriptor);

	/**
	 * Remove this non-root descriptor from its parent and remove all the
	 * children from this descriptor.
	 *
	 * <p>If this method is invoked on a {@linkplain #isRoot root} descriptor,
	 * this method must throw a {@link org.junit.platform.commons.JUnitException
	 * JUnitException} explaining that a root cannot be removed from the
	 * hierarchy.
	 */
	void removeFromHierarchy();

	/**
	 * Remove this descriptor from the hierarchy unless it is a root or contains
	 * tests.
	 *
	 * <p>A concrete {@link TestEngine} may override this method in order to
	 * implement a different algorithm or to skip pruning altogether.
	 *
	 * @see #isRoot()
	 * @see #containsTests(TestDescriptor)
	 * @see #removeFromHierarchy()
	 */
	default void prune() {
		if (!TestDescriptor.containsTests(this)) {
			removeFromHierarchy();
		}
	}

	/**
	 * Applies the given {@code Consumer} to the subtree starting with this
	 * descriptor in a top-down manner.
	 *
	 * @param consumer the {@code Consumer} to apply; never {@code null}
	 */
	default void applyInSubtreeTopDown(Consumer<TestDescriptorMutable> consumer) {
		Preconditions.notNull(consumer, "consumer must not be null");

		consumer.accept(this);

		// Create a copy of the set in order to avoid a ConcurrentModificationException
		LinkedHashSet<? extends TestDescriptorMutable> children = new LinkedHashSet<>(getChildren());
		for (TestDescriptorMutable child : children) {
			child.applyInSubtreeTopDown(consumer);
		}
	}

	/**
	 * Applies the given {@code Consumer} to the subtree starting with this
	 * descriptor in a bottom-up manner.
	 *
	 * @param consumer the {@code Consumer} to apply; never {@code null}
	 */
	default void applyInSubtreeBottomUp(Consumer<TestDescriptorMutable> consumer) {
		Preconditions.notNull(consumer, "consumer must not be null");

		// Create a copy of the set in order to avoid a ConcurrentModificationException
		LinkedHashSet<? extends TestDescriptorMutable> children = new LinkedHashSet<>(getChildren());
		for (TestDescriptorMutable child : children) {
			child.applyInSubtreeBottomUp(consumer);
		}

		consumer.accept(this);
	}
}
