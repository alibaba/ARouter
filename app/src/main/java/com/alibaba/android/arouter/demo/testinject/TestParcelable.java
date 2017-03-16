package com.alibaba.android.arouter.demo.testinject;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * TODO:Feature
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/3/16 下午4:42
 */
public class TestParcelable implements Parcelable {
    public String name;
    public int id;

    public TestParcelable() {
    }

    public TestParcelable(String name, int id) {
        this.name = name;
        this.id = id;
    }

    protected TestParcelable(Parcel in) {
        name = in.readString();
        id = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TestParcelable> CREATOR = new Creator<TestParcelable>() {
        @Override
        public TestParcelable createFromParcel(Parcel in) {
            return new TestParcelable(in);
        }

        @Override
        public TestParcelable[] newArray(int size) {
            return new TestParcelable[size];
        }
    };
}
