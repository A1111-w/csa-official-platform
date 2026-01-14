package com.csa.official.common.annotation;

import com.csa.official.modules.sys.enums.ContributionType;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogContribution {
    ContributionType type(); // 贡献类型

    String detail() default ""; // 描述模板
}
