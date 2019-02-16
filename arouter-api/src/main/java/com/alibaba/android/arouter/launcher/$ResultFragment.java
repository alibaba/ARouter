package com.alibaba.android.arouter.launcher;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;

import static com.alibaba.android.arouter.launcher.$ResultFragmentV4.generateCode;

/**
 * Just for launch activity and return result.
 *
 * @author act262@gmail.com
 * @deprecated Replace with {@link $ResultFragmentV4}
 */
@Deprecated
public class $ResultFragment extends Fragment {

    protected static void launch(Activity host, Intent intent, Bundle optionsBundle, OnResultCallback callback) {
        $ResultFragment fragment = new $ResultFragment();
        fragment.callback = callback;
        fragment.targetIntent = intent;
        fragment.targetBundle = optionsBundle;

        host.getFragmentManager()
                .beginTransaction()
                .add(fragment, $ResultFragment.class.getSimpleName())
                .commitAllowingStateLoss();
    }

    private Intent targetIntent;
    private Bundle targetBundle;
    private OnResultCallback callback;

    private int requestCode = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestCode = generateCode();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            startActivityForResult(targetIntent, requestCode, targetBundle);
        } else {
            startActivityForResult(targetIntent, requestCode);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // response current request
        if (callback != null && requestCode == this.requestCode) {
            callback.onResult(resultCode, data);
        }
    }
}
