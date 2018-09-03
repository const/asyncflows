package org.asyncflows.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation means that this framework class is public only because of java inter-package visibility limitations.
 */
@Documented
@Target(value = {
        ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PACKAGE, ElementType.FIELD
})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Internal {
}
