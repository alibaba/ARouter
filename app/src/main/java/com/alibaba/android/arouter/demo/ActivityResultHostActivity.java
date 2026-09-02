package com.alibaba.android.arouter.demo;

import android.app.Activity;
import android.content.Intent;

import androidx.core.app.ActivityCompat;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.NavigationCallback;
import com.alibaba.android.arouter.facade.callback.NavigationLauncher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Host activity used by the demo's navigation result tests.
 */
public class ActivityResultHostActivity extends Activity {
    static final int REQUEST_CODE = 905;

    private final CountDownLatch resultReceived = new CountDownLatch(1);
    private volatile int receivedRequestCode = -1;
    private volatile int receivedResultCode = Integer.MIN_VALUE;
    private volatile Intent receivedData;

    void navigateForResult(Postcard postcard, NavigationCallback callback) {
        postcard.navigation(getApplicationContext(), new NavigationLauncher() {
            @Override
            public void launch(Intent intent) {
                ActivityCompat.startActivityForResult(
                        ActivityResultHostActivity.this,
                        intent,
                        REQUEST_CODE,
                        null
                );
            }
        }, callback);
    }

    void navigateWithLegacyRequestCode(Postcard postcard, NavigationCallback callback) {
        postcard.navigation(this, REQUEST_CODE, callback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        receivedRequestCode = requestCode;
        receivedResultCode = resultCode;
        receivedData = data;
        resultReceived.countDown();
    }

    boolean awaitResult() throws InterruptedException {
        return resultReceived.await(5, TimeUnit.SECONDS);
    }

    int getReceivedRequestCode() {
        return receivedRequestCode;
    }

    int getReceivedResultCode() {
        return receivedResultCode;
    }

    Intent getReceivedData() {
        return receivedData;
    }
}
