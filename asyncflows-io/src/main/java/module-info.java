module org.asyncflows.io {
    requires org.slf4j;
    requires org.asyncflows.core;
    requires java.compiler;
    exports org.asyncflows.io;
    exports org.asyncflows.io.adapters.blocking;
    exports org.asyncflows.io.adapters.nio;
    exports org.asyncflows.io.file;
    exports org.asyncflows.io.file.nio;
    exports org.asyncflows.io.net;
    exports org.asyncflows.io.net.async;
    exports org.asyncflows.io.net.blocking;
    exports org.asyncflows.io.net.selector;
    exports org.asyncflows.io.net.tls;
    exports org.asyncflows.io.text;
    exports org.asyncflows.io.util;
}