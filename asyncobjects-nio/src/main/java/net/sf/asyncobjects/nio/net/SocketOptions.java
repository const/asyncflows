package net.sf.asyncobjects.nio.net;

import net.sf.asyncobjects.core.data.Tuple2;

/**
 * Socket options. This class allows preparing options in the batch and to set them using
 * {@link ASocket#setOptions(SocketOptions)}. The null value for the the property means
 * that corresponding option is not set.
 */
public final class SocketOptions implements Cloneable {
    /**
     * The option for {@link java.net.Socket#setTcpNoDelay(boolean)}.
     */
    private Boolean tpcNoDelay;
    /**
     * The option for {@link java.net.Socket#setSoTimeout(int)}.
     */
    private Integer timeout;
    /**
     * The option for {@link java.net.Socket#setSendBufferSize(int)}.
     */
    private Integer sendBufferSize;
    /**
     * The option for {@link java.net.Socket#setReceiveBufferSize(int)}.
     */
    private Integer receiveBufferSize;
    /**
     * The option for {@link java.net.Socket#setSoLinger(boolean, int)}.
     */
    private Tuple2<Boolean, Integer> linger;
    /**
     * The option for {@link java.net.Socket#setOOBInline(boolean)}.
     */
    private Boolean oobInline;
    /**
     * The option for {@link java.net.Socket#setKeepAlive(boolean)}.
     */
    private Boolean keepAlive;
    /**
     * The option for {@link java.net.Socket#setTrafficClass(int)}.
     */
    private Integer trafficClass;

    /**
     * @return the option for {@link java.net.Socket#setTcpNoDelay(boolean)}.
     */
    public Boolean getTpcNoDelay() {
        return tpcNoDelay;
    }

    /**
     * Prepare the option for {@link java.net.Socket#setTcpNoDelay(boolean)}.
     *
     * @param tpcNoDelay the option value
     */
    public void setTpcNoDelay(final Boolean tpcNoDelay) {
        this.tpcNoDelay = tpcNoDelay;
    }

    /**
     * @return the option for {@link java.net.Socket#setSoTimeout(int)}.
     */
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * Prepare the option for {@link java.net.Socket#setSoTimeout(int)}.
     *
     * @param timeout the option value
     */
    public void setTimeout(final Integer timeout) {
        this.timeout = timeout;
    }

    /**
     * @return the option for {@link java.net.Socket#setSendBufferSize(int)}.
     */
    public Integer getSendBufferSize() {
        return sendBufferSize;
    }

    /**
     * Prepare the option for {@link java.net.Socket#setSendBufferSize(int)}.
     *
     * @param sendBufferSize the option value
     */
    public void setSendBufferSize(final Integer sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    /**
     * @return the option for {@link java.net.Socket#setReceiveBufferSize(int)}.
     */
    public Integer getReceiveBufferSize() {
        return receiveBufferSize;
    }

    /**
     * Prepare the option for {@link java.net.Socket#setReceiveBufferSize(int)}.
     *
     * @param receiveBufferSize the option value
     */
    public void setReceiveBufferSize(final Integer receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    /**
     * @return the option for {@link java.net.Socket#setSoLinger(boolean, int)}.
     */
    public Tuple2<Boolean, Integer> getLinger() {
        return linger;
    }

    /**
     * Prepare the option for {@link java.net.Socket#setSoLinger(boolean, int)}.
     *
     * @param linger the option value
     */
    public void setLinger(final Tuple2<Boolean, Integer> linger) {
        this.linger = linger;
    }

    /**
     * @return the option for {@link java.net.Socket#setOOBInline(boolean)}.
     */
    public Boolean getOobInline() {
        return oobInline;
    }

    /**
     * Prepare the option for {@link java.net.Socket#setOOBInline(boolean)}.
     *
     * @param oobInline the option value
     */
    public void setOobInline(final Boolean oobInline) {
        this.oobInline = oobInline;
    }

    /**
     * @return the option for {@link java.net.Socket#setKeepAlive(boolean)}.
     */
    public Boolean getKeepAlive() {
        return keepAlive;
    }

    /**
     * Prepare the option for {@link java.net.Socket#setKeepAlive(boolean)}.
     *
     * @param keepAlive the option value
     */
    public void setKeepAlive(final Boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * @return the option for {@link java.net.Socket#setTrafficClass(int)}.
     */
    public Integer getTrafficClass() {
        return trafficClass;
    }

    /**
     * Prepare the option for {@link java.net.Socket#setTrafficClass(int)}.
     *
     * @param trafficClass the option value
     */
    public void setTrafficClass(final Integer trafficClass) {
        this.trafficClass = trafficClass;
    }

    @Override
    public SocketOptions clone() {
        try {
            return (SocketOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Unexpected exception from clone", e);
        }
    }
}
