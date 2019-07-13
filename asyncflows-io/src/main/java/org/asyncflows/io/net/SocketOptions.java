/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.io.net;

import org.asyncflows.core.data.Tuple2;

/**
 * Socket options. This class allows preparing options in the batch and to set them using
 * {@link ASocket#setOptions(SocketOptions)} and {@link ADatagramSocket#setOptions(SocketOptions)}.
 * The null value for the the property means that corresponding option is not set.
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
     * The option for {@link java.net.DatagramSocket#setBroadcast(boolean)}.
     */
    private Boolean broadcast;
    /**
     * The option for {@link java.net.DatagramSocket#setReuseAddress(boolean)}.
     */
    private Boolean reuseAddress;

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

    /**
     * @return value socket option {@link java.net.DatagramSocket#setBroadcast(boolean)}.
     */
    public Boolean getBroadcast() {
        return broadcast;
    }

    /**
     * Prepare the option for {@link java.net.DatagramSocket#setBroadcast(boolean)}.
     *
     * @param broadcast the option value
     */
    public void setBroadcast(final Boolean broadcast) {
        this.broadcast = broadcast;
    }

    /**
     * @return value of {@link java.net.DatagramSocket#setReuseAddress(boolean)}.
     */
    public Boolean getReuseAddress() {
        return reuseAddress;
    }

    /**
     * Prepare the option for {@link java.net.DatagramSocket#setReuseAddress(boolean)}.
     *
     * @param reuseAddress the option value
     */
    public void setReuseAddress(final Boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
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
