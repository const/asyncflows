/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

package org.asyncflows.core.util.control

import org.asyncflows.core.CoreFlows.*
import org.asyncflows.core.Promise
import org.asyncflows.core.util.CoreFlowsAll.aAll
import org.asyncflows.core.util.CoreFlowsResource.aTry
import org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile
import org.asyncflows.io.AInput
import org.asyncflows.io.AOutput
import org.asyncflows.io.IOUtil
import org.asyncflows.io.IOUtil.aTryChannel
import org.asyncflows.io.net.ASocket
import org.asyncflows.io.net.SocketOptions
import org.asyncflows.io.net.selector.SelectorVatUtil.doAsyncIo
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer

private val LOG = LoggerFactory.getLogger("echo.server")

fun copyData(input: AInput<ByteBuffer>, output: AOutput<ByteBuffer>, bufferSize: Int): Promise<Long> {
    val buffer = ByteBuffer.allocate(bufferSize)
    val result = LongArray(1)
    return aSeqWhile {
        input.read(buffer).flatMap { value ->
            if (IOUtil.isEof(value)) {
                aFalse()
            } else {
                result[0] = result[0] + value
                buffer.flip()
                output.write(buffer).thenFlatGet {
                    buffer.compact()
                    aTrue()
                }
            }
        }
    }.thenGet { result[0] }
}

fun main() {
    val socketOptions = SocketOptions()
    socketOptions.tpcNoDelay = true
    doAsyncIo { socketFactory ->
        aTry(socketFactory.makeServerSocket()).run { serverSocket ->
            serverSocket.bind(InetSocketAddress(22222), 100).thenFlatGet {
                serverSocket.setDefaultOptions(socketOptions)
                val count = LongArray(1)
                aSeqWhile {
                    serverSocket.accept().map { t: ASocket ->
                        // run handling in parallel
                        val start = System.currentTimeMillis()
                        val id = count[0]++
                        aTryChannel(t).run { c, input, out ->
                            aAll {
                                copyData(input, out, 4096).toOutcomePromise()
                            }.and {
                                c.remoteAddress
                            }.map { copy, address ->
                                val duration = System.currentTimeMillis() - start
                                if (copy.isSuccess) {
                                    LOG.info(
                                        "Request " + id + " handled: from = " + address
                                                + " duration = " + duration + " amount = " + copy.value()
                                    )
                                } else {
                                    LOG.error(
                                        "Request " + id + " failed: from = "
                                                + address + " duration = " + duration,
                                        copy.failure()
                                    )
                                }
                                aVoid()
                            }
                        }
                        true
                    }
                }
            }
        }
    }
}
