package com.alibaba.android.arouter.compiler.processor;

import com.alibaba.android.arouter.compiler.utils.Consts;
import com.alibaba.android.arouter.compiler.utils.CollectionUtils;
import com.alibaba.android.arouter.compiler.utils.MapUtils;
import com.alibaba.android.arouter.compiler.utils.StringUtils;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.enums.TypeKind;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.alibaba.android.arouter.compiler.utils.Consts.*;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Processor used to create autowired helper
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/2/20 下午5:56
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({ANNOTATION_TYPE_AUTOWIRED})
public class AutowiredProcessor extends BaseProcessor {
    private Map<TypeElement, List<Element>> parentAndChild = new HashMap<>();   // Contain field need autowired and his super class.
    private static final ClassName ARouterClass = ClassName.get("com.alibaba.android.arouter.launcher", "ARouter");
    private static final ClassName AndroidLog = ClassName.get("android.util", "Log");
    private static final ClassName AndroidBundle = ClassName.get("android.os", "Bundle");
    private static final ClassName TypeWrapperClass = ClassName.get("com.alibaba.android.arouter.facade.model", "TypeWrapper");

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        logger.info(">>> AutowiredProcessor init. <<<");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (CollectionUtils.isNotEmpty(set)) {
            try {
                logger.info(">>> Found autowired field, start... <<<");
                categories(roundEnvironment.getElementsAnnotatedWith(Autowired.class));
                generateHelper();

            } catch (Exception e) {
                logger.error(e);
            }
            return true;
        }

        return false;
    }

    private void generateHelper() throws IOException, IllegalAccessException {
        TypeElement type_ISyringe = elementUtils.getTypeElement(ISYRINGE);
        TypeElement type_JsonService = elementUtils.getTypeElement(JSON_SERVICE);
        TypeMirror iProvider = getTypeMirror(Consts.IPROVIDER);
        TypeMirror activityTm = getTypeMirror(Consts.ACTIVITY);
        TypeMirror fragmentTm = getTypeMirror(Consts.FRAGMENT);
        TypeMirror fragmentTmV4 = getTypeMirror(Consts.FRAGMENT_V4);
        TypeMirror fragmentTmAndroidX = getTypeMirror(Consts.FRAGMENT_ANDROIDX);

        // Build input param name.
        ParameterSpec objectParamSpec = ParameterSpec.builder(TypeName.OBJECT, "target").build();

        if (MapUtils.isNotEmpty(parentAndChild)) {
            for (Map.Entry<TypeElement, List<Element>> entry : parentAndChild.entrySet()) {
                // Build method : 'inject'
                MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder(METHOD_INJECT)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(objectParamSpec);

                TypeElement parent = entry.getKey();
                List<Element> childs = entry.getValue();

                String qualifiedName = parent.getQualifiedName().toString();
                String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
                String fileName = parent.getSimpleName() + NAME_OF_AUTOWIRED;

                logger.info(">>> Start process " + childs.size() + " field in " + parent.getSimpleName() + " ... <<<");

                TypeSpec.Builder helper = TypeSpec.classBuilder(fileName)
                        .addJavadoc(WARNING_TIPS)
                        .addSuperinterface(ClassName.get(type_ISyringe))
                        .addModifiers(PUBLIC);

                FieldSpec jsonServiceField = FieldSpec.builder(TypeName.get(type_JsonService.asType()), "serializationService", Modifier.PRIVATE).build();
                helper.addField(jsonServiceField);

                injectMethodBuilder.addStatement("serializationService = $T.getInstance().navigation($T.class)", ARouterClass, ClassName.get(type_JsonService));
                injectMethodBuilder.addStatement("$T substitute = ($T)target", ClassName.get(parent), ClassName.get(parent));

                boolean hasAutowiredValues = false;
                for (Element element : childs) {
                    if (!isSubtypeOf(element.asType(), iProvider)) {
                        hasAutowiredValues = true;
                        break;
                    }
                }

                if (hasAutowiredValues) {
                    if (isSubtypeOf(parent.asType(), activityTm)) {
                        injectMethodBuilder.addStatement(
                                "$T bundle = substitute.getIntent() == null ? null : substitute.getIntent().getExtras()",
                                AndroidBundle
                        );
                    } else if (isSubtypeOf(parent.asType(), fragmentTm)
                            || isSubtypeOf(parent.asType(), fragmentTmV4)
                            || isSubtypeOf(parent.asType(), fragmentTmAndroidX)) {
                        injectMethodBuilder.addStatement("$T bundle = substitute.getArguments()", AndroidBundle);
                    } else {
                        throw new IllegalAccessException("The fields need autowired from intent, its parent must be activity or fragment! ["
                                + parent.getQualifiedName() + "]");
                    }
                }

                // Generate method body, start inject.
                for (Element element : childs) {
                    Autowired fieldConfig = element.getAnnotation(Autowired.class);
                    String fieldName = element.getSimpleName().toString();
                    if (isSubtypeOf(element.asType(), iProvider)) {  // It's provider
                        if ("".equals(fieldConfig.name())) {    // User has not set service path, then use byType.

                            // Getter
                            injectMethodBuilder.addStatement(
                                    "substitute." + fieldName + " = $T.getInstance().navigation($T.class)",
                                    ARouterClass,
                                    ClassName.get(element.asType())
                            );
                        } else {    // use byName
                            // Getter
                            injectMethodBuilder.addStatement(
                                    "substitute." + fieldName + " = ($T)$T.getInstance().build($S).navigation()",
                                    ClassName.get(element.asType()),
                                    ARouterClass,
                                    fieldConfig.name()
                            );
                        }

                        // Validator
                        if (fieldConfig.required()) {
                            injectMethodBuilder.beginControlFlow("if (substitute." + fieldName + " == null)");
                            injectMethodBuilder.addStatement(
                                    "throw new RuntimeException(\"The field '" + fieldName + "' is null, in class '\" + $T.class.getName() + \"!\")", ClassName.get(parent));
                            injectMethodBuilder.endControlFlow();
                        }
                    } else {    // It's normal intent value
                        String paramName = StringUtils.isEmpty(fieldConfig.name()) ? fieldName : fieldConfig.name();
                        int type = typeUtils.typeExchange(element);

                        injectMethodBuilder.beginControlFlow("if (null != bundle && bundle.containsKey($S))", paramName);
                        if (type == TypeKind.OBJECT.ordinal()) {
                            injectMethodBuilder.beginControlFlow("if (null != serializationService)");
                            TypeName fieldType = TypeName.get(element.asType());
                            String valueName = fieldName + "Value";
                            injectMethodBuilder.addStatement(
                                    "$T " + valueName + " = serializationService.parseObject(bundle.getString($S), new $T<$T>(){}.getType())",
                                    fieldType,
                                    paramName,
                                    TypeWrapperClass,
                                    fieldType
                            );
                            injectMethodBuilder.beginControlFlow("if (null != " + valueName + ")");
                            injectMethodBuilder.addStatement("substitute." + fieldName + " = " + valueName);
                            injectMethodBuilder.endControlFlow();
                            injectMethodBuilder.nextControlFlow("else");
                            injectMethodBuilder.addStatement(
                                    "$T.e(\"" + Consts.TAG + "\", \"You want automatic inject the field '" + fieldName + "' in class '$T' , then you should implement 'SerializationService' to support object auto inject!\")", AndroidLog, ClassName.get(parent));
                            injectMethodBuilder.endControlFlow();
                        } else if (element.asType().getKind().isPrimitive()) {
                            injectMethodBuilder.addStatement(
                                    "substitute." + fieldName + " = bundle." + getBundleGetter(type) + "($S, substitute." + fieldName + ")",
                                    paramName
                            );
                        } else {
                            injectMethodBuilder.addStatement(
                                    "substitute." + fieldName + " = ($T) bundle.get($S)",
                                    TypeName.get(element.asType()),
                                    paramName
                            );
                        }
                        injectMethodBuilder.endControlFlow();

                        // Validator
                        if (fieldConfig.required() && !element.asType().getKind().isPrimitive()) {  // Primitive wont be check.
                            injectMethodBuilder.beginControlFlow("if (null == substitute." + fieldName + ")");
                            injectMethodBuilder.addStatement(
                                    "$T.e(\"" + Consts.TAG + "\", \"The field '" + fieldName + "' is null, in class '\" + $T.class.getName() + \"!\")", AndroidLog, ClassName.get(parent));
                            injectMethodBuilder.endControlFlow();
                        }
                    }
                }

                helper.addMethod(injectMethodBuilder.build());

                // Generate autowire helper
                JavaFile.builder(packageName, helper.build()).build().writeTo(mFiler);

                logger.info(">>> " + parent.getSimpleName() + " has been processed, " + fileName + " has been generated. <<<");
            }

            logger.info(">>> Autowired processor stop. <<<");
        }
    }

    private String getBundleGetter(int type) {
        switch (TypeKind.values()[type]) {
            case BOOLEAN:
                return "getBoolean";
            case BYTE:
                return "getByte";
            case SHORT:
                return "getShort";
            case INT:
                return "getInt";
            case LONG:
                return "getLong";
            case CHAR:
                return "getChar";
            case FLOAT:
                return "getFloat";
            case DOUBLE:
                return "getDouble";
            default:
                throw new IllegalArgumentException("Unsupported primitive type: " + TypeKind.values()[type]);
        }
    }

    /**
     * Categories field, find his papa.
     *
     * @param elements Field need autowired
     */
    private void categories(Set<? extends Element> elements) throws IllegalAccessException {
        if (CollectionUtils.isNotEmpty(elements)) {
            for (Element element : elements) {
                TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

                if (element.getModifiers().contains(Modifier.PRIVATE)) {
                    throw new IllegalAccessException("The inject fields CAN NOT BE 'private'!!! please check field ["
                            + element.getSimpleName() + "] in class [" + enclosingElement.getQualifiedName() + "]");
                }

                if (parentAndChild.containsKey(enclosingElement)) { // Has categries
                    parentAndChild.get(enclosingElement).add(element);
                } else {
                    List<Element> childs = new ArrayList<>();
                    childs.add(element);
                    parentAndChild.put(enclosingElement, childs);
                }
            }

            logger.info("categories finished.");
        }
    }
}
