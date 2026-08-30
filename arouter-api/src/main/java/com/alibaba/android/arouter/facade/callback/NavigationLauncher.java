package com.alibaba.android.arouter.facade.callback;

import android.content.Intent;

/**
 * Delegates the launch of an activity route to a caller-owned launcher.
 *
 * <p>The caller is responsible for registering the launcher, keeping it bound to the correct
 * lifecycle, and receiving the activity result. ARouter invokes this callback on the main thread
 * after route completion, pretreatment, and interceptors have succeeded.</p>
 *
 * <p>This interface can be used as an adapter for AndroidX
 * {@code ActivityResultLauncher<Intent>} without requiring ARouter itself to depend on AndroidX.</p>
 *
 * <p>The caller also owns Activity Result API launch options. Options configured through
 * {@code Postcard.withOptionsCompat(...)} are consumed only by ARouter's built-in launch path.</p>
 *
 * @author Alex Liu
 * @since 1.6.0
 */
public interface NavigationLauncher {

    /**
     * Launch the completed activity-route intent.
     *
     * @param intent intent containing the resolved destination, extras, action, and flags
     */
    void launch(Intent intent);
}
