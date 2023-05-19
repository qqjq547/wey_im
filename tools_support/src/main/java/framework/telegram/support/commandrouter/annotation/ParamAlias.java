package framework.telegram.support.commandrouter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import framework.telegram.support.commandrouter.converter.StringConverter;
import framework.telegram.support.commandrouter.converter.ValueConverter;


/**
 * Annotation used to define a parameter alias.
 *
 * @author Masson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ParamAlias {
    /**
     * The name (use as key) of the parameter that will be used in raw command.
     */
    public String value();

    /**
     * The class of converter that used to convert value.
     * If null, default converter for the target type will be used.
     */
    public Class<? extends ValueConverter> converter() default StringConverter.class;

    /**
     * The default value of the parameter in raw String.
     * @return
     */
    public String defaultRaw() default "";
}
