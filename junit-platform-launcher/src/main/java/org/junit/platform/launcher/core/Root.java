/*
 * Copyright 2015-2020 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.junit.platform.engine.Filter.composeFilters;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

/**
 * Represents the root of all discovered {@link TestEngine TestEngines} and
 * their {@link TestDescriptor TestDescriptors}.
 *
 * @since 1.0
 */
class Root {

	private static final Logger logger = LoggerFactory.getLogger(Root.class);

	private final Map<TestEngine, TestDescriptor> testEngineDescriptors = new LinkedHashMap<>(4);
	private final ConfigurationParameters configurationParameters;

	Root(ConfigurationParameters configurationParameters) {
		this.configurationParameters = configurationParameters;
	}

	public ConfigurationParameters getConfigurationParameters() {
		return configurationParameters;
	}

	/**
	 * Add an {@code engine}'s root {@link TestDescriptor}.
	 */
	void add(TestEngine engine, TestDescriptor testDescriptor) {
		this.testEngineDescriptors.put(engine, testDescriptor);
	}

	Iterable<TestEngine> getTestEngines() {
		return this.testEngineDescriptors.keySet();
	}

	Collection<TestDescriptor> getEngineDescriptors() {
		return this.testEngineDescriptors.values();
	}

	TestDescriptor getTestDescriptorFor(TestEngine testEngine) {
		return this.testEngineDescriptors.get(testEngine);
	}

	void applyPostDiscoveryFilters(LauncherDiscoveryRequest discoveryRequest) {
		Filter<TestDescriptor> postDiscoveryFilter = composeFilters(discoveryRequest.getPostDiscoveryFilters());
		ExcludedTestDescriptors excludedTestDescriptors = new ExcludedTestDescriptors();
		TestDescriptor.Visitor removeExcludedTestDescriptors = descriptor -> {
			FilterResult filterResult = postDiscoveryFilter.apply(descriptor);
			if (!descriptor.isRoot() && isExcluded(descriptor, filterResult)) {
				String exclusionReason = filterResult.getReason().orElse("Unknown");
				excludedTestDescriptors.add(descriptor, exclusionReason);
				descriptor.removeFromHierarchy();
			}
		};
		acceptInAllTestEngines(removeExcludedTestDescriptors);
		excludedTestDescriptors.logExclusionReasons();
	}

	/**
	 * Prune all branches in the tree of {@link TestDescriptor TestDescriptors}
	 * that do not have executable tests.
	 *
	 * <p>If a {@link TestEngine} ends up with no {@code TestDescriptors} after
	 * pruning, it will <strong>not</strong> be removed.
	 */
	void prune() {
		acceptInAllTestEngines(TestDescriptor::prune);
	}

	private boolean isExcluded(TestDescriptor descriptor, FilterResult filterResult) {
		return descriptor.getChildren().isEmpty() && filterResult.excluded();
	}

	private void acceptInAllTestEngines(TestDescriptor.Visitor visitor) {
		this.testEngineDescriptors.values().forEach(descriptor -> descriptor.accept(visitor));
	}

	/**
	 * Stores the reasons behind the exclusion of test descriptors based on
	 * their tags, in order to log them.
	 *
	 * @since 5.7
	 */
	private static class ExcludedTestDescriptors {

		private final Map<String, List<TestDescriptor>> excludedTestDescriptorsByReason = new LinkedHashMap<>();

		void add(TestDescriptor testDescriptor, String exclusionReason) {
			excludedTestDescriptorsByReason //
					.computeIfAbsent(exclusionReason, list -> new LinkedList<>()) //
					.add(testDescriptor);
		}

		void logExclusionReasons() {
			excludedTestDescriptorsByReason.forEach((exclusionReason, testDescriptors) -> {
				String displayNames = testDescriptors.stream().map(TestDescriptor::getDisplayName).collect(
					Collectors.joining(", "));
				long containerCount = testDescriptors.stream().filter(TestDescriptor::isContainer).count();
				long methodCount = testDescriptors.stream().filter(TestDescriptor::isTest).count();
				logger.info(() -> String.format("%d containers and %d tests were %s", containerCount, methodCount,
					exclusionReason));
				logger.debug(() -> String.format("The following containers and tests were %s: %s", exclusionReason,
					displayNames));
			});
		}

	}

}
