package org.junit.jupiter.params;

import org.junit.jupiter.api.extension.ContainerTemplateInvocationContext;
import org.junit.jupiter.api.extension.ContainerTemplateInvocationContextProvider;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.stream.Stream;

class ParameterizedContainerExtension implements ContainerTemplateInvocationContextProvider {
    @Override
    public boolean supportsContainerTemplate(ExtensionContext context) {
        return false;
    }

    @Override
    public Stream<ContainerTemplateInvocationContext> provideContainerTemplateInvocationContexts(ExtensionContext context) {
        return null;
    }
}
