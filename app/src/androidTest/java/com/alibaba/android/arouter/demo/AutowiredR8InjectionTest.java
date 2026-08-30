package com.alibaba.android.arouter.demo;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.widget.TextView;

import com.alibaba.android.arouter.launcher.ARouter;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AutowiredR8InjectionTest {
    private Activity activity;

    @BeforeClass
    public static void initializeRouter() {
        Application application = (Application) InstrumentationRegistry.getTargetContext().getApplicationContext();
        ARouter.openDebug();
        ARouter.openLog();
        ARouter.init(application);
    }

    @After
    public void finishActivity() {
        if (null == activity) {
            return;
        }

        final Activity current = activity;
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                current.finish();
            }
        });
        instrumentation.waitForIdleSync();
    }

    @Test
    public void fragmentFieldsAreInjectedAfterMinification() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(InstrumentationRegistry.getTargetContext(), AutowiredFragmentHostActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity = instrumentation.startActivitySync(intent);

        final TextView result = (TextView) activity.findViewById(android.R.id.text1);
        final AtomicReference<String> text = new AtomicReference<String>();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                text.set(result.getText().toString());
            }
        });

        assertEquals("r8-user|27|true", text.get());
    }
}
