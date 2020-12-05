module org.asyncflows.apt {
    requires java.compiler;
    provides javax.annotation.processing.Processor with org.asyncflows.apt.AsynchronousProxyProcessor;
}