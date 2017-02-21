package com.alibaba.android.arouter.compiler.utils;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static com.alibaba.android.arouter.compiler.utils.Consts.BOOLEAN;
import static com.alibaba.android.arouter.compiler.utils.Consts.BYTE;
import static com.alibaba.android.arouter.compiler.utils.Consts.DOUBEL;
import static com.alibaba.android.arouter.compiler.utils.Consts.FLOAT;
import static com.alibaba.android.arouter.compiler.utils.Consts.INTEGER;
import static com.alibaba.android.arouter.compiler.utils.Consts.LONG;
import static com.alibaba.android.arouter.compiler.utils.Consts.SHORT;
import static com.alibaba.android.arouter.compiler.utils.Consts.STRING;

/**
 * Utils for type exchange
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/2/21 下午1:06
 */
public class TypeUtils {
    /**
     * Diagnostics out the true java type
     *
     * @param rawType Raw type
     * @return Type class of java
     */
    public static int typeExchange(TypeMirror rawType) {
        if (rawType.getKind().isPrimitive()) {  // is java base type
            return rawType.getKind().ordinal();
        }

        switch (rawType.toString()) {
            case BYTE:
                return TypeKind.BYTE.ordinal();
            case SHORT:
                return TypeKind.SHORT.ordinal();
            case INTEGER:
                return TypeKind.INT.ordinal();
            case LONG:
                return TypeKind.LONG.ordinal();
            case FLOAT:
                return TypeKind.FLOAT.ordinal();
            case DOUBEL:
                return TypeKind.DOUBLE.ordinal();
            case BOOLEAN:
                return TypeKind.BOOLEAN.ordinal();
            case STRING:
            default:
                return TypeKind.OTHER.ordinal();  // I say it was java.long.String

        }
    }
}
