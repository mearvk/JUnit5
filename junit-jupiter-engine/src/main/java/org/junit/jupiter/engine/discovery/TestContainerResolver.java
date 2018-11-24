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

import org.junit.jupiter.api.ContainerTemplate;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.ContainerTemplateTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsPotentialTestContainer;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * @since 5.0
 */
class TestContainerResolver implements ElementResolver {

	private static final IsPotentialTestContainer isPotentialTestContainer = new IsPotentialTestContainer();

	static final String SEGMENT_TYPE = "class";

	protected final ConfigurationParameters configurationParameters;

	public TestContainerResolver(ConfigurationParameters configurationParameters) {
		this.configurationParameters = configurationParameters;
	}

	@Override
	public Set<TestDescriptor> resolveElement(AnnotatedElement element, TestDescriptor parent) {
		if (!(element instanceof Class)) {
			return Collections.emptySet();
		}

		Class<?> clazz = (Class<?>) element;
		if (!isPotentialCandidate(clazz)) {
			return Collections.emptySet();
		}

		UniqueId uniqueId = createUniqueId(clazz, parent);
		return Collections.singleton(resolveClass(clazz, uniqueId));
	}

	@Override
	public Optional<TestDescriptor> resolveUniqueId(UniqueId.Segment segment, TestDescriptor parent) {

		if (!segment.getType().equals(getSegmentType())) {
			return Optional.empty();
		}

		if (!requiredParentType().isInstance(parent)) {
			return Optional.empty();
		}

		String className = getClassName(parent, segment.getValue());

		Optional<Class<?>> optionalContainerClass = ReflectionUtils.tryToLoadClass(className).toOptional();
		if (!optionalContainerClass.isPresent()) {
			return Optional.empty();
		}

		Class<?> containerClass = optionalContainerClass.get();
		if (!isPotentialCandidate(containerClass)) {
			return Optional.empty();
		}

		UniqueId uniqueId = createUniqueId(containerClass, parent);
		return Optional.of(resolveClass(containerClass, uniqueId));
	}

	protected Class<? extends TestDescriptor> requiredParentType() {
		return TestDescriptor.class;
	}

	protected String getClassName(TestDescriptor parent, String segmentValue) {
		return segmentValue;
	}

	protected String getSegmentType() {
		return SEGMENT_TYPE;
	}

	protected String getSegmentValue(Class<?> testClass) {
		return testClass.getName();
	}

	protected boolean isPotentialCandidate(Class<?> element) {
		return isPotentialTestContainer.test(element);
	}

	protected UniqueId createUniqueId(Class<?> testClass, TestDescriptor parent) {
		return parent.getUniqueId().append(getSegmentType(), getSegmentValue(testClass));
	}

	protected TestDescriptor resolveClass(Class<?> testClass, UniqueId uniqueId) {
		if (AnnotationUtils.isAnnotated(testClass, ContainerTemplate.class)) {
			return new ContainerTemplateTestDescriptor(uniqueId, testClass, this.configurationParameters);
		}
		return new ClassTestDescriptor(uniqueId, testClass, this.configurationParameters);
	}

}
