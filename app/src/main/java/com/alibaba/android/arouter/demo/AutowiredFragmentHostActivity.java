package com.alibaba.android.arouter.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.alibaba.android.arouter.demo.module1.AutowiredR8Fragment;
import com.alibaba.android.arouter.launcher.ARouter;

/**
 * Host activity used by the demo's R8 autowiring regression test.
 */
public class AutowiredFragmentHostActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AutowiredR8Fragment fragment = new AutowiredR8Fragment();
        Bundle arguments = new Bundle();
        arguments.putString("name", "r8-user");
        arguments.putInt("age", 27);
        arguments.putBoolean("boy", true);
        fragment.setArguments(arguments);
        ARouter.getInstance().inject(fragment);

        TextView result = new TextView(this);
        result.setId(android.R.id.text1);
        result.setText(fragment.getInjectedState());
        setContentView(result);
    }
}
