package com.alibaba.android.arouter.configcache;

import androidx.fragment.app.DialogFragment;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;

@Route(path = "/cache/androidx-dialog")
public final class AndroidXProbeDialogFragment extends DialogFragment {
    @Autowired
    public String source;
}
