package org.junit.jupiter.engine.descriptor;

import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.UniqueId;

public class ContainerTemplateTestDescriptor extends ClassBasedTestDescriptor {
    public ContainerTemplateTestDescriptor(UniqueId uniqueId, Class<?> testClass, ConfigurationParameters configurationParameters) {
        super(uniqueId, testClass, configurationParameters);
    }

    @Override
    public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) {
        return context;
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }
}
