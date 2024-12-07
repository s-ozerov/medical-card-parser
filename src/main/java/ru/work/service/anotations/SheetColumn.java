package ru.work.service.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SheetColumn {

    String name();

    String parseFromFormat() default "yyyy-MM-dd";

    boolean enabled() default true;

}
