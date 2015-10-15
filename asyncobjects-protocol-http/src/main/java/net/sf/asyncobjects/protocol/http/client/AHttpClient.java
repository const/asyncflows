package net.sf.asyncobjects.protocol.http.client;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;

/**
 * The client.
 */
public interface AHttpClient extends ACloseable {
    /**
     * @return run the next request
     */
    Promise<AHttpRequest> newRequest();
}
