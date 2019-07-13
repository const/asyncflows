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


import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The processor for asynchronous annotations.
 */
@SupportedAnnotationTypes(AsynchronousProxyProcessor.ASYNCHRONOUS_NAME)
public class AsynchronousProxyProcessor extends AbstractProcessor {

    public static final String ASYNCHRONOUS_NAME = "org.asyncflows.core.annotations.Asynchronous";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            final String name = annotation.getQualifiedName().toString();
            if (ASYNCHRONOUS_NAME.equals(name)) {
                final Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
                final Map<Boolean, List<Element>> annotatedMethods = annotatedElements.stream().collect(
                        Collectors.partitioningBy(element -> element.getKind() == ElementKind.INTERFACE
                                && element.asType() instanceof DeclaredType));
                final List<Element> interfaces = annotatedMethods.get(true);
                final List<Element> others = annotatedMethods.get(false);
                others.forEach(e -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@Asynchronous should be attached only to interface types ("
                                + annotation.getKind() + ")", e));
                for (final Element element : interfaces) {
                    final TypeElement typeElement = (TypeElement) element;
                    final TypeAnalyser analyser = new TypeAnalyser(processingEnv, typeElement);
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            "Generating proxy for " + analyser.getInterfaceQName());
                    try {
                        final JavaFileObject builderFile = processingEnv.getFiler()
                                .createSourceFile(analyser.getFactoryQName(), typeElement);
                        try (final PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                            new ProxyGenerator(out, analyser).generate();
                        }
                    } catch (Exception ex) {
                        throw new IllegalStateException("Failed to create proxy: "
                                + analyser.getInterfaceQName(), ex);
                    }
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public List<TypeParameterElement> getTypeParameters(TypeElement typeElement) {
        return (List<TypeParameterElement>) typeElement.getTypeParameters();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        // maybe some other reaction is need if do not support it actually
        return SourceVersion.latestSupported();
    }
}
