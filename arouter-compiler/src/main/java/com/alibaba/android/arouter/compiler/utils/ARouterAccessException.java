package com.alibaba.android.arouter.compiler.utils;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * Created by 张宇 on 2018/5/2.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */

public class ARouterAccessException extends IllegalAccessException {

    public ARouterAccessException(Element field, String modifier, String method) {
        super("The autowired fields CAN NOT BE '" + modifier + "' unless you provide a public " + method + " method!!! please check field ["
                + field.getSimpleName() + "] in class [" + ((TypeElement) field.getEnclosingElement()).getQualifiedName() + "]");
    }
}
