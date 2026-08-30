package com.alibaba.android.arouter.core;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.alibaba.android.arouter.facade.template.ILogger;
import com.alibaba.android.arouter.launcher.ARouter;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class InstrumentationHookIntentRobustnessInstrumentedTest {

    @BeforeClass
    public static void enableLegacyAutoInjection() {
        ARouter.enableAutoInject();
    }

    @Test
    public void normalParcelledExtrasStillInjectIntoTheActivity() throws Exception {
        Intent intent = parcelIntent(new Intent()
                .putExtra(ARouter.AUTO_INJECT, new String[]{"message"})
                .putExtra("message", "normal-value"));

        HookActivity activity = newActivity(intent);

        assertEquals("normal-value", activity.message);
    }

    @Test
    public void malformedParcelableIsActuallyRejectedWhenItsExtraIsRead() {
        Intent intent = parcelledIntentWithMalformedExtra();

        try {
            intent.getExtras().get("message");
            fail("The parcelled extra must be malformed for this Intent robustness test");
        } catch (BadParcelableException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void malformedExtrasDoNotAbortActivityInstantiation() throws Exception {
        HookActivity activity = newActivity(parcelledIntentWithMalformedExtra());

        assertNotNull(activity);
        assertNull(activity.message);
    }

    @Test
    public void forgedAutoInjectFieldDoesNotAbortActivityInstantiationWhenLoggerIsUnavailable() throws Exception {
        Intent intent = new Intent()
                .putExtra(ARouter.AUTO_INJECT, new String[]{"missingField"})
                .putExtra("missingField", "untrusted-value");

        ILogger previousLogger = ARouter.logger;
        HookActivity activity;
        try {
            ARouter.logger = null;
            activity = newActivity(intent);
        } finally {
            ARouter.logger = previousLogger;
        }

        assertNotNull(activity);
        assertNull(activity.message);
    }

    private static HookActivity newActivity(Intent intent) throws Exception {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final AtomicReference<HookActivity> activity = new AtomicReference<>();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Intent activityIntent = intent;
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    activity.set((HookActivity) new InstrumentationHook().newActivity(
                            InstrumentationHookIntentRobustnessInstrumentedTest.class.getClassLoader(),
                            HookActivity.class.getName(),
                            activityIntent
                    ));
                } catch (Throwable throwable) {
                    failure.set(throwable);
                }
            }
        });

        Throwable throwable = failure.get();
        if (throwable instanceof Exception) {
            throw (Exception) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        if (throwable != null) {
            throw new RuntimeException(throwable);
        }
        return activity.get();
    }

    private static Intent parcelledIntentWithMalformedExtra() {
        return parcelIntent(new Intent()
                .putExtra(ARouter.AUTO_INJECT, new String[]{"message"})
                .putExtra("message", new ThrowingParcelable()));
    }

    private static Intent parcelIntent(Intent source) {
        Parcel parcel = Parcel.obtain();
        try {
            source.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            Intent result = Intent.CREATOR.createFromParcel(parcel);
            result.setExtrasClassLoader(ThrowingParcelable.class.getClassLoader());
            return result;
        } finally {
            parcel.recycle();
        }
    }

    public static final class HookActivity extends Activity {
        public String message;
    }

    public static final class ThrowingParcelable implements Parcelable {
        public static final Creator<ThrowingParcelable> CREATOR = new Creator<ThrowingParcelable>() {
            @Override
            public ThrowingParcelable createFromParcel(Parcel source) {
                throw new BadParcelableException("malformed test payload");
            }

            @Override
            public ThrowingParcelable[] newArray(int size) {
                return new ThrowingParcelable[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(1);
        }
    }
}
