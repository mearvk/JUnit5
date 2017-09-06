/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.commons.util.ReflectionUtils.findMethods;
import static org.junit.platform.commons.util.ReflectionUtils.findNestedClasses;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.Filterable;
import org.junit.jupiter.engine.discovery.predicates.IsInnerClass;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.UniqueId.Segment;
import org.junit.platform.engine.support.descriptor.TestDescriptorMutable;

/**
 * @since 5.0
 */
class JavaElementsResolver {

	private static final Logger logger = LoggerFactory.getLogger(JavaElementsResolver.class);

	private static final IsInnerClass isInnerClass = new IsInnerClass();

	private final TestDescriptorMutable engineDescriptor;
	private final Set<ElementResolver> resolvers;

	JavaElementsResolver(TestDescriptorMutable engineDescriptor, Set<ElementResolver> resolvers) {
		this.engineDescriptor = engineDescriptor;
		this.resolvers = resolvers;
	}

	void resolveClass(Class<?> testClass) {
		Set<TestDescriptorMutable> resolvedDescriptors = resolveContainerWithParents(testClass);
		resolvedDescriptors.forEach(this::resolveChildren);

		if (resolvedDescriptors.isEmpty()) {
			logger.debug(() -> format("Class '%s' could not be resolved.", testClass.getName()));
		}
	}

	void resolveMethod(Class<?> testClass, Method testMethod) {
		Set<TestDescriptorMutable> potentialParents = resolveContainerWithParents(testClass);
		Set<TestDescriptorMutable> resolvedDescriptors = resolveForAllParents(testMethod, potentialParents);

		if (resolvedDescriptors.isEmpty()) {
			logger.debug(() -> format("Method '%s' could not be resolved.", testMethod.toGenericString()));
		}

		logMultipleTestDescriptorsForSingleElement(testMethod, resolvedDescriptors);
	}

	private Set<TestDescriptorMutable> resolveContainerWithParents(Class<?> testClass) {
		if (isInnerClass.test(testClass)) {
			Set<TestDescriptorMutable> potentialParents = resolveContainerWithParents(testClass.getDeclaringClass());
			return resolveForAllParents(testClass, potentialParents);
		}
		else {
			return resolveForAllParents(testClass, Collections.singleton(engineDescriptor));
		}
	}

	void resolveUniqueId(UniqueId uniqueId) {
		// Ignore Unique IDs from other test engines.
		if (JupiterTestEngine.ENGINE_ID.equals(uniqueId.getEngineId().orElse(null))) {
			Deque<TestDescriptorMutable> resolvedDescriptors = resolveAllSegments(uniqueId);
			handleResolvedDescriptorsForUniqueId(uniqueId, resolvedDescriptors);
		}
	}

	/**
	 * Attempt to resolve all segments for the supplied unique ID.
	 */
	private Deque<TestDescriptorMutable> resolveAllSegments(UniqueId uniqueId) {
		List<Segment> segments = uniqueId.getSegments();
		Deque<TestDescriptorMutable> resolvedDescriptors = new LinkedList<>();
		resolvedDescriptors.addFirst(this.engineDescriptor);

		for (int index = 1; index < segments.size() && resolvedDescriptors.size() == index; index++) {
			Segment segment = segments.get(index);
			TestDescriptorMutable parent = resolvedDescriptors.getLast();
			UniqueId partialUniqueId = parent.getUniqueId().append(segment);

			Optional<TestDescriptorMutable> resolvedDescriptor = findTestDescriptorByUniqueId(partialUniqueId);
			if (!resolvedDescriptor.isPresent()) {
				// @formatter:off
				resolvedDescriptor = this.resolvers.stream()
						.map(resolver -> resolver.resolveUniqueId(segment, parent))
						.filter(Optional::isPresent)
						.map(Optional::get)
						.findFirst();
				// @formatter:on
				resolvedDescriptor.ifPresent(parent::addChild);
			}
			resolvedDescriptor.ifPresent(resolvedDescriptors::addLast);
		}
		return resolvedDescriptors;
	}

	private void handleResolvedDescriptorsForUniqueId(UniqueId uniqueId, Deque<TestDescriptorMutable> resolvedDescriptors) {
		List<Segment> segments = uniqueId.getSegments();
		int numSegmentsToResolve = segments.size() - 1;
		int numSegmentsResolved = resolvedDescriptors.size() - 1;

		if (numSegmentsResolved == 0) {
			logger.warn(() -> format("Unique ID '%s' could not be resolved.", uniqueId));
		}
		else if (numSegmentsResolved != numSegmentsToResolve) {
			if (resolvedDescriptors.getLast() instanceof Filterable) {
				((Filterable) resolvedDescriptors.getLast()).getDynamicDescendantFilter().allow(uniqueId);
			}
			else {
				logger.warn(() -> {
					List<Segment> unresolved = segments.subList(1, segments.size()); // Remove engine ID
					unresolved = unresolved.subList(numSegmentsResolved, unresolved.size()); // Remove resolved segments
					return format("Unique ID '%s' could only be partially resolved. "
							+ "All resolved segments will be executed; however, the "
							+ "following segments could not be resolved: %s",
						uniqueId, unresolved);
				});
			}
		}
		else {
			resolveChildren(resolvedDescriptors.getLast());
		}
	}

	/**
	 * Attempt to resolve all segments for the supplied unique ID.
	 *
	 * @return the number of segments resolved
	 */
	private int resolveUniqueId(TestDescriptorMutable parent, List<Segment> remainingSegments) {
		if (remainingSegments.isEmpty()) {
			resolveChildren(parent);
			return 0;
		}

		Segment head = remainingSegments.remove(0);
		for (ElementResolver resolver : this.resolvers) {
			Optional<TestDescriptorMutable> resolvedDescriptor = resolver.resolveUniqueId(head, parent);
			if (!resolvedDescriptor.isPresent()) {
				continue;
			}

			Optional<TestDescriptorMutable> foundTestDescriptor = findTestDescriptorByUniqueId(
				resolvedDescriptor.get().getUniqueId());
			TestDescriptorMutable descriptor = foundTestDescriptor.orElseGet(() -> {
				TestDescriptorMutable newDescriptor = resolvedDescriptor.get();
				parent.addChild(newDescriptor);
				return newDescriptor;
			});
			return 1 + resolveUniqueId(descriptor, remainingSegments);
		}

		return 0;
	}

	private Set<TestDescriptorMutable> resolveContainerWithChildren(Class<?> containerClass,
			Set<TestDescriptorMutable> potentialParents) {

		Set<TestDescriptorMutable> resolvedDescriptors = resolveForAllParents(containerClass, potentialParents);
		resolvedDescriptors.forEach(this::resolveChildren);
		return resolvedDescriptors;
	}

	private Set<TestDescriptorMutable> resolveForAllParents(AnnotatedElement element,
			Set<TestDescriptorMutable> potentialParents) {
		Set<TestDescriptorMutable> resolvedDescriptors = new HashSet<>();
		potentialParents.forEach(parent -> resolvedDescriptors.addAll(resolve(element, parent)));
		// @formatter:off
		resolvedDescriptors.stream()
				.filter(Filterable.class::isInstance)
				.map(Filterable.class::cast)
				.forEach(testDescriptor -> testDescriptor.getDynamicDescendantFilter().allowAll());
		// @formatter:on
		return resolvedDescriptors;
	}

	private void resolveChildren(TestDescriptorMutable descriptor) {
		if (descriptor instanceof ClassTestDescriptor) {
			Class<?> testClass = ((ClassTestDescriptor) descriptor).getTestClass();
			resolveContainedMethods(descriptor, testClass);
			resolveContainedNestedClasses(descriptor, testClass);
		}
	}

	private void resolveContainedNestedClasses(TestDescriptorMutable containerDescriptor, Class<?> clazz) {
		List<Class<?>> nestedClassesCandidates = findNestedClasses(clazz, isInnerClass);
		nestedClassesCandidates.forEach(
			nestedClass -> resolveContainerWithChildren(nestedClass, Collections.singleton(containerDescriptor)));
	}

	private void resolveContainedMethods(TestDescriptorMutable containerDescriptor, Class<?> testClass) {
		List<Method> testMethodCandidates = findMethods(testClass, ReflectionUtils::isNotPrivate);
		testMethodCandidates.forEach(method -> resolve(method, containerDescriptor));
	}

	private Set<TestDescriptorMutable> resolve(AnnotatedElement element, TestDescriptorMutable parent) {
		Set<TestDescriptorMutable> descriptors = this.resolvers.stream() //
				.map(resolver -> tryToResolveWithResolver(element, parent, resolver)) //
				.filter(testDescriptors -> !testDescriptors.isEmpty()) //
				.flatMap(Collection::stream) //
				.collect(toSet());

		logMultipleTestDescriptorsForSingleElement(element, descriptors);

		return descriptors;
	}

	private Set<TestDescriptorMutable> tryToResolveWithResolver(AnnotatedElement element, TestDescriptorMutable parent,
			ElementResolver resolver) {

		Set<TestDescriptorMutable> resolvedDescriptors = resolver.resolveElement(element, parent);
		Set<TestDescriptorMutable> result = new LinkedHashSet<>();

		resolvedDescriptors.forEach(testDescriptor -> {
			Optional<TestDescriptorMutable> existingTestDescriptor = findTestDescriptorByUniqueId(
				testDescriptor.getUniqueId());
			if (existingTestDescriptor.isPresent()) {
				result.add(existingTestDescriptor.get());
			}
			else {
				parent.addChild(testDescriptor);
				result.add(testDescriptor);
			}
		});

		return result;
	}

	@SuppressWarnings("unchecked")
	private Optional<TestDescriptorMutable> findTestDescriptorByUniqueId(UniqueId uniqueId) {
		return (Optional<TestDescriptorMutable>) this.engineDescriptor.findByUniqueId(uniqueId);
	}

	private void logMultipleTestDescriptorsForSingleElement(AnnotatedElement element,
			Set<TestDescriptorMutable> descriptors) {
		if (descriptors.size() > 1 && element instanceof Method) {
			Method method = (Method) element;
			logger.warn(() -> String.format(
				"Possible configuration error: method [%s] resulted in multiple TestDescriptors %s. "
						+ "This is typically the result of annotating a method with multiple competing annotations "
						+ "such as @Test, @RepeatedTest, @ParameterizedTest, @TestFactory, etc.",
				method.toGenericString(), descriptors.stream().map(d -> d.getClass().getName()).collect(toList())));
		}
	}
}
