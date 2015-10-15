package net.sf.asyncobjects.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation means that API is highly unstable (even more unstable then the rest of the framework).
 * And it will very likely change in the future.
 */
@Documented
@Target(value = {
        ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PACKAGE, ElementType.FIELD
})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Experimental {
}
