package net.sf.asyncobjects.nio.codec;

import net.sf.asyncobjects.core.Promise;

/**
 * The stateful object decoder interface.
 *
 * @param <B> the buffer
 * @param <O> the object type
 */
public interface ObjectDecoder<B, O> {
    /**
     * Get decoded object. Note that the object could be got only once.
     *
     * @return the object to decode in case (in case of {@link DecodeResult#OBJECT_READY})
     */
    O getObject();

    /**
     * @return the failure related to decoding (in case of {@link DecodeResult#FAILURE})
     */
    Exception getFailure();

    /**
     * Decode the buffer. Note that some decoders needs to access external data or perform actions on other threads
     * before returning the result. The decoder should consume only elements that are actually used by
     * the object. Other decoders could be run on the buffer later.
     *
     * @param buffer the buffer (ready for get)
     * @param eof    true, if this is the end of the stream
     * @return the promise decode status
     */
    Promise<DecodeResult> decode(B buffer, boolean eof);

    /**
     * The decode result.
     */
    enum DecodeResult {
        /**
         * An object is ready.
         */
        OBJECT_READY,
        /**
         * The more data is needed.
         */
        BUFFER_UNDERFLOW,
        /**
         * The end of file, no more objects are expected.
         */
        EOF,
        /**
         * The failure is encountered.
         */
        FAILURE
    }
}
