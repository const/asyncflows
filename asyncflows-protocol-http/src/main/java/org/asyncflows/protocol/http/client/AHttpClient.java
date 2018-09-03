package org.asyncflows.protocol.http.client;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

/**
 * The client.
 */
public interface AHttpClient extends ACloseable {
    /**
     * @return run the next request
     */
    Promise<AHttpRequest> newRequest();
}
