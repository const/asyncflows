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

package org.asyncflows.apt;

import java.io.PrintWriter;
import java.util.Objects;

/**
 * The generator for the proxy.
 */
@SuppressWarnings("squid:S1192")
public final class ProxyGenerator {
    private static final String OBJECT = "java.lang.Object";
    private static final String VAT = "org.asyncflows.core.vats.Vat";
    private static final String IDENT = "    ";
    private final PrintWriter writer;
    private final TypeAnalyser type;
    private int identLevel;
    private boolean lineStarted;

    /**
     * The constructor.
     *
     * @param writer the writer
     * @param type   the type
     */
    public ProxyGenerator(PrintWriter writer, TypeAnalyser type) {
        this.writer = writer;
        this.type = type;
    }

    /**
     * Generate proxy factory.
     */
    public void generate() {
        line("package " + type.getPackageName() + ";");
        line();
        line("/**");
        line(" * The asynchronous proxy factory for {@link " + type.getInterfaceQName() + "}.");
        line(" */");
        line("@javax.annotation.Generated(\"" + AsynchronousProxyProcessor.class.getName() + "\")");
        write("public final class " + type.getFactoryName()
                + " implements java.util.function.BiFunction<" + VAT + ", " + OBJECT + ", " + OBJECT
                + ">, org.asyncflows.core.util.AsynchronousService"
        ).block(() -> {
            line("public static final " + type.getFactoryName() + " INSTANCE = new " + type.getFactoryName() + "();");
            line();
            createProxyJDoc();
            write("public static " + type.getProxyTypeParametersWithBounds() + " "
                    + type.getInterfaceType() + " createProxy(" + VAT + " vat, "
                    + type.getInterfaceType() + " service)").block(() -> {
                line("return new " + type.getProxyName()
                        + type.getProxyTypeParametersWithoutBounds() + "(vat, service);");
            });

            line();
            createProxyJDoc();
            write("public " + type.getProxyTypeParametersWithBounds() + " "
                    + type.getInterfaceType() + " export(" + VAT + " vat, "
                    + type.getInterfaceType() + " service)").block(() -> {
                line("return createProxy(vat, service);");
            });
            line();
            line("@Override");
            line("@SuppressWarnings(\"unchecked\")");
            write("public " + OBJECT + " apply(" + VAT + " vat, " + OBJECT + " service)").block(() -> {
                line("return createProxy(vat, (" + type.getInterfaceQName() + ") service);");
            });
            line();
            generateProxyClass();
        });
    }

    public void createProxyJDoc() {
        line("/**");
        line(" * Create a proxy.");
        line(" *");
        line(" * @param vat     the vat");
        line(" * @param service the service to export");
        for (String typeParameter : type.getTypeParameters()) {
            line(" * @param <" + typeParameter + "> a type parameter");
        }
        line(" * @return the exported service");
        line(" */");
    }

    private void generateProxyClass() {
        final String typeParametersWithBounds = type.getProxyTypeParametersWithBounds();
        line("@javax.annotation.Generated(\"" + AsynchronousProxyProcessor.class.getName() + "\")");
        write("private static final class " + type.getProxyName() + typeParametersWithBounds + " implements " + type.getInterfaceType()).block(() -> {
            line("private final " + VAT + " vat;");
            line("private final " + type.getInterfaceType() + " service;");
            line();
            write("private " + type.getProxyName() + "(final " + VAT + " vat, final "
                    + type.getInterfaceType() + " service)").block(() -> {
                line("java.util.Objects.requireNonNull(vat);");
                line("java.util.Objects.requireNonNull(service);");
                line("this.vat = vat;");
                line("this.service = service;");
            });
            line();
            line("@Override");
            write("public int hashCode()").block(() ->
                    line("return System.identityHashCode(service);"));
            line();
            line("@Override");
            write("public boolean equals(" + OBJECT + " o2)").block(() ->
                    line("return this == o2 "
                            + "|| (o2 != null && o2.getClass() == getClass() && (("
                            + type.getProxyName() + ")o2).service == this.service);"));
            for (TypeAnalyser.MethodInfo method : type.getAllMethods()) {
                line();
                line("@Override");
                write("public " + method.getSignature()).block(() -> {
                    if (method.isPromise()) {
                        line("return org.asyncflows.core.CoreFlows.aLater(this.vat, () -> this.service." + method.getInvoke() + ");");
                    } else if (method.isOneWay()) {
                        line("org.asyncflows.core.CoreFlows.aOneWay(this.vat, () -> this.service." + method.getInvoke() + ");");
                    } else {
                        line("throw new java.lang.UnsupportedOperationException();");
                    }
                });
            }
        });
    }

    private ProxyGenerator write(Object value) {
        if (!lineStarted) {
            lineStarted = true;
            for (int i = 0; i < identLevel; i++) {
                writer.write(IDENT);
            }
        }
        writer.write(Objects.toString(value));
        return this;
    }


    private void line() {
        writer.println();
        lineStarted = false;
    }

    private void line(Object value) {
        write(value).line();
    }

    private void block(Runnable block) {
        start();
        block.run();
        end();
    }

    private void start() {
        line(" {");
        identLevel++;
    }

    private void end() {
        identLevel--;
        line("}");
    }
}
