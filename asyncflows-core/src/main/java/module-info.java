module org.asyncflows.core {
    requires org.slf4j;
    requires java.compiler;
    exports org.asyncflows.core;
    exports org.asyncflows.core.annotations;
    exports org.asyncflows.core.context;
    exports org.asyncflows.core.context.spi;
    exports org.asyncflows.core.context.util;
    exports org.asyncflows.core.data;
    exports org.asyncflows.core.function;
    exports org.asyncflows.core.streams;
    exports org.asyncflows.core.time;
    exports org.asyncflows.core.trace;
    exports org.asyncflows.core.util;
    exports org.asyncflows.core.vats;
    uses org.asyncflows.core.trace.PromiseTraceProvider;
    provides org.asyncflows.core.trace.PromiseTraceProvider with
            org.asyncflows.core.trace.PromiseTraceExceptionProvider,
            org.asyncflows.core.trace.PromiseTraceNopProvider;
}