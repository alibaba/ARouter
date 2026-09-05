package com.alibaba.android.arouter.core;

import android.app.Application;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.alibaba.android.arouter.demo.module1.testservice.FailingProvider;
import com.alibaba.android.arouter.exception.HandlerException;
import com.alibaba.android.arouter.exception.InitException;
import com.alibaba.android.arouter.launcher.ARouter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class RoutingFailureDiagnosticsInstrumentedTest {

    @Before
    public void resetRouter() {
        ARouter.openDebug();
        try {
            ARouter.getInstance().destroy();
        } catch (InitException ignored) {
            // The first test in a fresh process has no initialized router to destroy.
        }

        Application application = (Application) InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getApplicationContext();
        ARouter.init(application);
    }

    @Test
    public void providerInitializationFailurePreservesActionableCause() {
        try {
            ARouter.getInstance().build("/diagnostics/failing-provider").navigation();
            fail("Expected provider initialization to fail");
        } catch (HandlerException failure) {
            assertTrue(failure.getMessage().contains(FailingProvider.class.getName()));
            assertTrue(failure.getMessage().contains(IllegalStateException.class.getName()));
            assertTrue(failure.getMessage().contains(FailingProvider.FAILURE_MESSAGE));
            assertSame(IllegalStateException.class, failure.getCause().getClass());
            assertEquals(FailingProvider.FAILURE_MESSAGE, failure.getCause().getMessage());
        }
    }
}
