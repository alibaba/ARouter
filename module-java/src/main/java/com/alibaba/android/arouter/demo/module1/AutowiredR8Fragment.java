package com.alibaba.android.arouter.demo.module1;

import androidx.fragment.app.Fragment;

import com.alibaba.android.arouter.facade.annotation.Autowired;

/**
 * Fragment used by the R8 autowiring regression test.
 */
public class AutowiredR8Fragment extends Fragment {
    @Autowired
    String name;

    @Autowired
    int age = 10;

    @Autowired(name = "boy")
    boolean girl;

    public String getInjectedState() {
        return name + "|" + age + "|" + girl;
    }
}
