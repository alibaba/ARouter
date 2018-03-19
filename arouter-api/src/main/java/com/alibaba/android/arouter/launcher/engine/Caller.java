package com.alibaba.android.arouter.launcher.engine;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * auth zzc
 * date 2018/3/19
 * desc ${desc}
 */

public interface Caller {
    void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options);

    void startActivity(Intent intent, @Nullable Bundle options);

    void overridePendingTransition(int enterAnim, int exitAnim);

    @Nullable
    Context getContext();
}
