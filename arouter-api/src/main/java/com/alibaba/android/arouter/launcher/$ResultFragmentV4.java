package com.alibaba.android.arouter.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import java.util.Random;

/**
 * Just for launch activity and return result.
 *
 * @author act262@gmail.com
 */
public class $ResultFragmentV4 extends Fragment {

    protected static void launch(FragmentActivity host, Intent intent, Bundle optionsBundle, OnResultCallback callback) {
        $ResultFragmentV4 fragment = new $ResultFragmentV4();
        fragment.callback = callback;
        fragment.targetIntent = intent;
        fragment.targetBundle = optionsBundle;

        host.getSupportFragmentManager()
                .beginTransaction()
                .add(fragment, $ResultFragmentV4.class.getSimpleName())
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

        startActivityForResult(targetIntent, requestCode, targetBundle);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // response current request
        if (callback != null && requestCode == this.requestCode) {
            callback.onResult(resultCode, data);
        }
    }

    private static Random codeRandom = new Random();

    protected static int generateCode() {
        // because v4 fragment limit, requestCode <= 0xffff0000
        return codeRandom.nextInt(0xFFFF);
    }
}
