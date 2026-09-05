package com.alibaba.android.arouter.launcher;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.NavigationCallback;
import com.alibaba.android.arouter.facade.callback.NavigationLauncher;
import com.alibaba.android.arouter.facade.enums.RouteType;
import com.alibaba.android.arouter.facade.template.ILogger;
import com.alibaba.android.arouter.utils.Consts;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FragmentInstantiationDiagnosticsTest {
    private ILogger originalLogger;
    private ILogger logger;
    private _ARouter router;
    private Method navigation;

    @Before
    public void setUp() throws Exception {
        originalLogger = _ARouter.logger;
        logger = mock(ILogger.class);
        _ARouter.logger = logger;
        Constructor<_ARouter> constructor = _ARouter.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        router = constructor.newInstance();
        navigation = _ARouter.class.getDeclaredMethod("_navigation", Postcard.class,
                int.class, NavigationLauncher.class, NavigationCallback.class);
        navigation.setAccessible(true);
    }

    @After
    public void tearDown() {
        _ARouter.logger = originalLogger;
    }

    @Test
    public void missingPublicNoArgumentConstructorLogsExceptionAndReturnsNull() throws Exception {
        Throwable failure = navigateFailingTarget(MissingDefaultConstructor.class);
        assertTrue(failure instanceof NoSuchMethodException);
    }

    @Test
    public void throwingConstructorPreservesCauseAndReturnsNull() throws Exception {
        Throwable failure = navigateFailingTarget(ThrowingConstructor.class);
        assertTrue(failure instanceof InvocationTargetException);
        assertTrue(failure.getCause() instanceof IllegalStateException);
        assertEquals("fragment-constructor-detail", failure.getCause().getMessage());
    }

    @Test
    public void nullDestinationLogsFailureAndReturnsNull() throws Exception {
        assertTrue(navigateFailingTarget(null) instanceof NullPointerException);
    }

    private Throwable navigateFailingTarget(Class<?> destination) throws Exception {
        Postcard postcard = new Postcard();
        postcard.setType(RouteType.FRAGMENT);
        postcard.setDestination(destination);
        assertNull(navigation.invoke(router, postcard, -1, null, null));

        ArgumentCaptor<Throwable> cause = ArgumentCaptor.forClass(Throwable.class);
        String destinationName = destination == null ? "null" : destination.getName();
        verify(logger).error(eq(Consts.TAG), contains(destinationName), cause.capture());
        return cause.getValue();
    }

    public static final class MissingDefaultConstructor {
        public MissingDefaultConstructor(String ignored) {}
    }

    public static final class ThrowingConstructor {
        public ThrowingConstructor() {
            throw new IllegalStateException("fragment-constructor-detail");
        }
    }
}
