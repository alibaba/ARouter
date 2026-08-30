package com.alibaba.android.arouter.launcher;

import android.content.Context;

import com.alibaba.android.arouter.facade.callback.NavigationLauncher;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class NavigationContextTest {

    @Test
    public void usesApplicationContextWhenCallerDoesNotProvideOne() {
        Context applicationContext = mock(Context.class);

        assertSame(applicationContext, _ARouter.resolveNavigationContext(null, applicationContext));
    }

    @Test
    public void preservesExplicitNavigationContext() {
        Context applicationContext = mock(Context.class);
        Context activityContext = mock(Context.class);

        assertSame(activityContext, _ARouter.resolveNavigationContext(activityContext, applicationContext));
    }

    @Test
    public void applicationContextNeedsNewTaskForBuiltInLaunch() {
        Context applicationContext = mock(Context.class);

        assertTrue(_ARouter.shouldAddNewTaskFlag(applicationContext, null));
    }

    @Test
    public void callerOwnedLauncherOwnsTaskFlags() {
        Context applicationContext = mock(Context.class);
        NavigationLauncher launcher = mock(NavigationLauncher.class);

        assertFalse(_ARouter.shouldAddNewTaskFlag(applicationContext, launcher));
    }
}
