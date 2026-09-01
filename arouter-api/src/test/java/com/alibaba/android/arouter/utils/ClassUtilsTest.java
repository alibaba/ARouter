package com.alibaba.android.arouter.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClassUtilsTest {
    @Test
    public void processApplicationInfoDoesNotDependOnPackageManagerBinder() throws Exception {
        Context context = mock(Context.class);
        ApplicationInfo applicationInfo = mock(ApplicationInfo.class);

        when(context.getApplicationInfo()).thenReturn(applicationInfo);

        assertSame(applicationInfo, ClassUtils.getProcessApplicationInfo(context));
        verify(context, never()).getPackageManager();
    }
}
