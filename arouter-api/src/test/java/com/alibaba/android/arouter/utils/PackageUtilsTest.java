package com.alibaba.android.arouter.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;

import static com.alibaba.android.arouter.utils.Consts.AROUTER_SP_CACHE_KEY;
import static com.alibaba.android.arouter.utils.Consts.LAST_UPDATE_TIME;
import static com.alibaba.android.arouter.utils.Consts.LAST_VERSION_CODE;
import static com.alibaba.android.arouter.utils.Consts.LAST_VERSION_NAME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PackageUtilsTest {
    private Context context;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private PackageInfo packageInfo;

    @Before
    public void setUp() throws Exception {
        context = mock(Context.class);
        preferences = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);
        PackageManager packageManager = mock(PackageManager.class);
        packageInfo = mock(PackageInfo.class);

        packageInfo.versionName = "1.0";
        packageInfo.versionCode = 1;
        packageInfo.lastUpdateTime = 200L;

        when(context.getPackageName()).thenReturn("repro.cache");
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getSharedPreferences(AROUTER_SP_CACHE_KEY, Context.MODE_PRIVATE))
                .thenReturn(preferences);
        when(packageManager.getPackageInfo(eq("repro.cache"), eq(PackageManager.GET_CONFIGURATIONS)))
                .thenReturn(packageInfo);
        when(preferences.getString(LAST_VERSION_NAME, null)).thenReturn("1.0");
        when(preferences.getInt(LAST_VERSION_CODE, -1)).thenReturn(1);
        when(preferences.edit()).thenReturn(editor);
        when(editor.putString(LAST_VERSION_NAME, "1.0")).thenReturn(editor);
        when(editor.putInt(LAST_VERSION_CODE, 1)).thenReturn(editor);
        when(editor.putLong(LAST_UPDATE_TIME, 200L)).thenReturn(editor);
    }

    @Test
    public void sameVersionAndInstallDoesNotInvalidateCache() {
        when(preferences.getLong(LAST_UPDATE_TIME, -1L)).thenReturn(200L);

        assertFalse(PackageUtils.isNewVersion(context));
        PackageUtils.updateVersion(context);

        verify(editor, never()).apply();
    }

    @Test
    public void reinstalledApkInvalidatesCacheEvenWhenVersionIsUnchanged() {
        when(preferences.getLong(LAST_UPDATE_TIME, -1L)).thenReturn(100L);

        assertTrue(PackageUtils.isNewVersion(context));
        PackageUtils.updateVersion(context);

        verify(editor).putString(LAST_VERSION_NAME, "1.0");
        verify(editor).putInt(LAST_VERSION_CODE, 1);
        verify(editor).putLong(LAST_UPDATE_TIME, 200L);
        verify(editor).apply();
    }

    @Test
    public void existingCacheWithoutInstallTimeIsMigrated() {
        when(preferences.getLong(LAST_UPDATE_TIME, -1L)).thenReturn(-1L);

        assertTrue(PackageUtils.isNewVersion(context));
        PackageUtils.updateVersion(context);

        verify(editor).putLong(LAST_UPDATE_TIME, 200L);
        verify(editor).apply();
    }
}
