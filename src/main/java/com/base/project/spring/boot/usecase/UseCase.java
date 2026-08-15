package com.base.project.spring.boot.usecase;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * Marks a class as a single-purpose application use case (one public {@code execute(...)} method per class) rather than
 * a general-purpose "service". Meta-annotated with {@link Component} so it's still picked up by component scanning like
 * any other bean.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface UseCase {
}
