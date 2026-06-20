package org.example.roomsched.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogAction {
    /**
     * 动作名称，如：提交预约、审批通过
     */
    String value() default "";
}
