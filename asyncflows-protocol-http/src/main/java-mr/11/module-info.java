module org.asyncflows.protocol.http {
    requires org.slf4j;
    requires org.asyncflows.core;
    requires org.asyncflows.io;
    requires org.asyncflows.protocol;
    exports org.asyncflows.protocol.http;
    exports org.asyncflows.protocol.http.client;
    exports org.asyncflows.protocol.http.client.core;
    exports org.asyncflows.protocol.http.common;
    exports org.asyncflows.protocol.http.common.content;
    exports org.asyncflows.protocol.http.common.headers;
    exports org.asyncflows.protocol.http.server;
    exports org.asyncflows.protocol.http.server.core;
    exports org.asyncflows.protocol.http.server.util;
}