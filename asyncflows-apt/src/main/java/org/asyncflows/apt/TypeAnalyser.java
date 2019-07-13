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

package org.asyncflows.apt;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.TypeKindVisitor8;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Analyzer for types.
 */
public final class TypeAnalyser {
    private final ProcessingEnvironment processingEnvironment;
    private final TypeElement typeElement;

    /**
     * The constructor.
     *
     * @param processingEnvironment the processing environment
     * @param typeElement           the type element
     */
    public TypeAnalyser(ProcessingEnvironment processingEnvironment, TypeElement typeElement) {
        this.processingEnvironment = processingEnvironment;
        this.typeElement = typeElement;
    }

    /**
     * Execute action for each element and execute additional action between elements.
     *
     * @param collection the collection to iterate
     * @param action     the action executed for each element
     * @param between    the action executed before non-first element
     * @param <T>        the element type
     */
    private static <T> void forEachAsList(Iterable<T> collection, Consumer<T> action, Runnable between) {
        boolean isFirst = true;
        for (T t : collection) {
            if (isFirst) {
                isFirst = false;
            } else {
                between.run();
            }
            action.accept(t);
        }
    }

    /**
     * @return the name of package for interface
     */
    public String getPackageName() {
        return processingEnvironment.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
    }

    /**
     * @return the proxy name
     */
    public String getProxyName() {
        return typeElement.getSimpleName() + "AsyncProxy";
    }

    /**
     * @return the proxy type parameters with bounds
     */
    public String getProxyTypeParametersWithBounds() {
        String typeParameters = typeElement.getTypeParameters().stream()
                .map(t -> typeWithBounds(t.asType()))
                .collect(Collectors.joining(", "));
        return typeParameters.isEmpty() ? "" : "<" + typeParameters + ">";
    }

    /**
     * @return the type parameters
     */
    public List<String> getTypeParameters() {
        return typeElement.getTypeParameters().stream()
                .map(p -> ((TypeParameterElement) p))
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    /**
     * @return the interface name
     */
    public String getInterfaceQName() {
        return typeElement.getQualifiedName().toString();
    }

    /**
     * @return the interface type
     */
    public String getInterfaceType() {
        return getInterfaceQName() + getProxyTypeParametersWithoutBounds();
    }

    /**
     * @return the proxy type parameters without bounds
     */
    public String getProxyTypeParametersWithoutBounds() {
        String typeParameters = typeElement.getTypeParameters().stream()
                .map(p -> ((TypeParameterElement) p))
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        return typeParameters.isEmpty() ? "" : "<" + typeParameters + ">";
    }

    /**
     * @return the list of all methods except for object methods.
     */
    public List<MethodInfo> getAllMethods() {
        return processingEnvironment.getElementUtils().getAllMembers(typeElement).stream()
                .filter(e -> e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                .filter(e -> !e.isDefault() && !e.getModifiers().contains(Modifier.STATIC)
                        && !e.getEnclosingElement().asType().toString().equals("java.lang.Object"))
                .map(MethodInfo::new)
                .collect(Collectors.toList());
    }

    /**
     * Type with bounds.
     *
     * @param type the type
     * @return the type string
     */
    private String typeWithBounds(TypeMirror type) {
        final StringBuilder out = new StringBuilder();
        printType(type, out, true);
        return out.toString();
    }

    /**
     * Print type to string builder.
     *
     * @param type        the type
     * @param out         the builder
     * @param printBounds if true, bounds are printed for top level type variables
     */
    @SuppressWarnings({"squid:S3776", "squid:MaximumInheritanceDepth"})
    private void printType(TypeMirror type, StringBuilder out, boolean printBounds) {
        type.accept(new TypeKindVisitor8<Void, Boolean>() {
            @Override
            public Void visitIntersection(IntersectionType t, Boolean bounds) {
                forEachAsList(t.getBounds(), b -> b.accept(this, false), () -> out.append(" & "));
                return null;
            }

            @Override
            public Void visitUnion(UnionType t, Boolean bounds) {
                forEachAsList(t.getAlternatives(), b -> b.accept(this, false), () -> out.append(" | "));
                return null;
            }

            @Override
            public Void visitPrimitive(PrimitiveType t, Boolean bounds) {
                out.append(t.getKind().name().toLowerCase(Locale.US));
                return null;
            }

            @Override
            public Void visitNoTypeAsVoid(NoType t, Boolean bounds) {
                out.append("void");
                return null;
            }

            @Override
            public Void visitArray(ArrayType t, Boolean bounds) {
                t.getComponentType().accept(this, false);
                out.append("[]");
                return null;
            }

            @Override
            public Void visitDeclared(DeclaredType t, Boolean bounds) {
                out.append(((TypeElement) t.asElement()).getQualifiedName());
                if (!t.getTypeArguments().isEmpty()) {
                    out.append('<');
                    forEachAsList(t.getTypeArguments(), a -> a.accept(this, false), () -> out.append(", "));
                    out.append('>');
                }
                return null;
            }

            @Override
            public Void visitTypeVariable(TypeVariable t, Boolean bounds) {
                out.append(t.asElement().getSimpleName());
                if (bounds) {
                    if (t.getLowerBound() instanceof NullType) {
                        if (isNonEmptyType(t.getUpperBound())) {
                            out.append(" extends ");
                            t.getUpperBound().accept(this, false);
                        }
                    } else {
                        out.append(" super ");
                        t.getLowerBound().accept(this, false);
                    }
                }
                return null;
            }

            @Override
            public Void visitWildcard(WildcardType t, Boolean bounds) {
                out.append("?");
                if (t.getSuperBound() instanceof NullType) {
                    if (isNonEmptyType(t)) {
                        out.append(" extends ");
                        t.getExtendsBound().accept(this, false);
                    }
                } else {
                    out.append(" super ");
                    t.getSuperBound().accept(this, false);
                }
                return null;
            }
        }, printBounds);
    }

    private boolean isNonEmptyType(TypeMirror t) {
        if (t.getKind() == TypeKind.NULL) {
            return false;
        }
        if (t.getKind() != TypeKind.DECLARED) {
            return true;
        }
        return !((TypeElement) ((DeclaredType) t).asElement()).getQualifiedName().toString().equals(Object.class.getName());
    }

    /**
     * @return the qualified name of the factory
     */
    public String getFactoryQName() {
        return getPackageName() + "." + getFactoryName();
    }

    /**
     * @return the simple name of factory
     */
    public String getFactoryName() {
        return typeElement.getSimpleName() + "ProxyFactory";
    }


    /**
     * The method information.
     */
    public final class MethodInfo {
        private final ExecutableElement element;
        private final ExecutableType type;

        /**
         * The constructor.
         *
         * @param element the element
         */
        public MethodInfo(ExecutableElement element) {
            this.element = element;
            this.type = (ExecutableType) processingEnvironment.getTypeUtils().asMemberOf((DeclaredType) typeElement.asType(), element);
        }

        /**
         * @return the method signature including type variables.
         */
        public String getSignature() {
            StringBuilder b = new StringBuilder();
            final List<? extends TypeVariable> typeVariables = type.getTypeVariables();
            if (!typeVariables.isEmpty()) {
                b.append('<');
                forEachAsList(typeVariables, t -> printType(t, b, true), () -> b.append(", "));
                b.append("> ");
            }
            printType(type.getReturnType(), b, false);
            b.append(' ');
            b.append(element.getSimpleName());
            b.append('(');
            for (int i = 0; i < element.getParameters().size(); i++) {
                if (i > 0) {
                    b.append(", ");
                }
                printType(type.getParameterTypes().get(i), b, false);
                b.append(' ');
                b.append(element.getParameters().get(i).getSimpleName());
            }
            b.append(')');
            return b.toString();
        }

        /**
         * @return the method invocation with same parameter names as in signature
         */
        public String getInvoke() {
            StringBuilder b = new StringBuilder();
            b.append(element.getSimpleName());
            b.append('(');
            forEachAsList(element.getParameters(), p -> b.append(p.getSimpleName()), () -> b.append(", "));
            b.append(')');
            return b.toString();
        }

        /**
         * @return true if promise method
         */
        public boolean isPromise() {
            return "org.asyncflows.core.Promise".equals(getErasedReturnType());
        }

        /**
         * @return true if one-way method
         */
        public boolean isOneWay() {
            return type.getReturnType().getKind() == TypeKind.VOID;
        }

        /**
         * @return get erased return type
         */
        private String getErasedReturnType() {
            return processingEnvironment.getTypeUtils().erasure(type.getReturnType()).toString();
        }
    }

}
