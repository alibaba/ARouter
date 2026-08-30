package com.alibaba.android.arouter.compiler.processor;

import com.alibaba.android.arouter.compiler.entity.RouteMeta;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import javax.lang.model.element.Element;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

public class RuntimeModelBoundaryTest {

    @Test
    public void routeMetaDoesNotReferenceJavaCompilerModel() throws IOException {
        Class<?> runtimeModel = com.alibaba.android.arouter.facade.model.RouteMeta.class;
        String resourceName = "/" + runtimeModel.getName().replace('.', '/') + ".class";
        InputStream input = runtimeModel.getResourceAsStream(resourceName);
        if (input == null) {
            throw new AssertionError("RouteMeta class resource not found");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        input.close();

        String constantPool = new String(output.toByteArray(), StandardCharsets.ISO_8859_1);
        assertFalse(constantPool.contains("javax/lang/model"));
    }

    @Test
    public void compilerMetadataRemainsInCompilerArtifact() {
        Element compilerElement = (Element) Proxy.newProxyInstance(
                Element.class.getClassLoader(),
                new Class<?>[]{Element.class},
                (proxy, method, args) -> null
        );
        RouteMeta routeMeta = new RouteMeta().setRawType(compilerElement);

        assertSame(compilerElement, routeMeta.getRawType());
    }
}
