package com.alibaba.android.arouter.launcher;

import android.content.Context;
import android.content.Intent;

import com.alibaba.android.arouter.facade.callback.NavigationLauncher;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void resultRequestRemovesForwardResultAndPreservesOtherFlags() {
        int flags = Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_CLEAR_TOP;

        assertEquals(
                Intent.FLAG_ACTIVITY_CLEAR_TOP,
                _ARouter.sanitizeActivityFlags(flags, 0, null)
        );
    }

    @Test
    public void callerOwnedLauncherRemovesForwardResultFlag() {
        int flags = Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_CLEAR_TOP;
        NavigationLauncher launcher = mock(NavigationLauncher.class);

        assertEquals(
                Intent.FLAG_ACTIVITY_CLEAR_TOP,
                _ARouter.sanitizeActivityFlags(flags, -1, launcher)
        );
    }

    @Test
    public void normalNavigationPreservesForwardResultFlagWithoutResultLauncher() {
        int flags = Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_CLEAR_TOP;

        assertEquals(flags, _ARouter.sanitizeActivityFlags(flags, -1, null));
    }
}
