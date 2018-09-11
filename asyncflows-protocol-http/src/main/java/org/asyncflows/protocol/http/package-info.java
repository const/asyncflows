/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

/**
 * <p>The utility classes for handling HTTP protocol. The classes provider a quite low level access to
 * the HTTP protocol. However, the higher level functionality could be implemented over it and some
 * utilities actually do this.</p>
 * <p>Applicable standards:</p>
 * <ul>
 * <li><a href="http://tools.ietf.org/html/rfc7230">RFC 7230: Hypertext Transfer Protocol (HTTP/1.1):
 * Message Syntax and Routing</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc7231">RFC 7231: Hypertext Transfer Protocol (HTTP/1.1):
 * Semantics and Content</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc7232">RFC 7232: Hypertext Transfer Protocol (HTTP/1.1):
 * Conditional Requests</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc7233">RFC 7233: Hypertext Transfer Protocol (HTTP/1.1):
 * Range Requests</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc7234">RFC 7234: Hypertext Transfer Protocol (HTTP/1.1):
 * Caching</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc7235">RFC 7235: Hypertext Transfer Protocol (HTTP/1.1):
 * Authentication</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc1945">RFC 1945: Hypertext Transfer Protocol -- HTTP/1.0
 * (obsolete)</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc2616">RFC 2616: Hypertext Transfer Protocol -- HTTP/1.1
 * (obsolete)</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc6585">RFC 6585: Additional HTTP Status Codes</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc2818">RFC 2818: HTTP Over TLS (not-implemented)</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc2817">RFC 2817: Upgrading to TLS Within HTTP/1.1
 * (not-implemented)</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc2145">RFC 2145: Use and Interpretation of HTTP Version
 * Numbers</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc5322">RFC 5322: Internet Message Format
 * (redefined in RFC7230 for http 1.1)</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc2047">RFC 2047: MIME (Multipurpose Internet Mail Extensions) Part
 * Three: Message Header Extensions for Non-ASCII Text</a></li>
 * <li><a href="http://tools.ietf.org/html/rfc2231">RFC 2231: MIME Parameter Value and Encoded Word Extensions:
 * Character Sets, Languages, and Continuations</a></li>
 * </ul>
 * <p>Related Draft Standards:</p>
 * <ul>
 * <li><a href="http://tools.ietf.org/html/draft-ietf-httpbis-http2-14#ref-HTTP-p1">Hypertext Transfer Protocol
 * version 2.0 draft-ietf-httpbis-http2-14</a></li>
 * </ul>
 */
package org.asyncflows.protocol.http;
