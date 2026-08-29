package com.alibaba.android.arouter.launcher;

import android.content.Context;

import org.junit.Test;

import static org.junit.Assert.assertSame;
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
}
