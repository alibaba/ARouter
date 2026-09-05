package com.alibaba.android.arouter.configcache;

import androidx.fragment.app.Fragment;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.annotation.Autowired;

@Route(path = "/cache/androidx-fragment")
public final class AndroidXProbeFragment extends Fragment {
    @Autowired
    public String source;
}
