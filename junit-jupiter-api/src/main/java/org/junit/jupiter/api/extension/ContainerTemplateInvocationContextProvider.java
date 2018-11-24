package org.junit.jupiter.api.extension;

import java.util.stream.Stream;

public interface ContainerTemplateInvocationContextProvider extends Extension {

    boolean supportsContainerTemplate(ExtensionContext context);

    Stream<ContainerTemplateInvocationContext> provideContainerTemplateInvocationContexts(ExtensionContext context);

}
