/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit.engine;

public class EngineTestKitAssertions {

	/**
	 * Creates a new instance of {@link EventAssert}.
	 *
	 * @param event the actual {@code Event}
	 * @return the created assertion object
	 */
	public static EventAssert assertThat(Event event) {
		return new EventAssert(event);
	}

	/**
	 * Creates a new instance of {@link ExecutionAssert}.
	 *
	 * @param execution the actual {@code Execution}
	 * @return the created assertion object
	 */
	public static ExecutionAssert assertThat(Execution execution) {
		return new ExecutionAssert(execution);
	}

	/**
	 * Creates a new instance of {@link TerminationInfoAssert}.
	 *
	 * @param terminationInfo the actual {@code TerminationInfo}
	 * @return the created assertion object
	 */
	public static TerminationInfoAssert assertThat(TerminationInfo terminationInfo) {
		return new TerminationInfoAssert(terminationInfo);
	}

}
