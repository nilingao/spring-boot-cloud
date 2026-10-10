package cn.com.nla.common.video.basic.interfaces;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageElement {
    String value();

    String subVal() default "";
}
