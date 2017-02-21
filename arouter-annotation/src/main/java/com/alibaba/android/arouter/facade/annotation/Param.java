package com.alibaba.android.arouter.facade.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used for mark param of page.
 * THIS ANNOTATION WAS DEPRECATED, USE 'Autowired' PLEASE!
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 2016/11/22 18:01
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
@Deprecated
public @interface Param {
    /**
     * The field name
     */
    String name() default "";

    /**
     * The description of the field
     */
    String desc() default "No desc.";
}
