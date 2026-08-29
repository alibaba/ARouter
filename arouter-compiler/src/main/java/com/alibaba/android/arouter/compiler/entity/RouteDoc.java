package com.alibaba.android.arouter.compiler.entity;

import com.alibaba.android.arouter.compiler.utils.StringUtils;

import java.util.List;

/**
 * Description route info, used for generate router map
 *
 * @author zhilong <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 2018/8/9 11:59 AM
 */
public class RouteDoc {
    private String group;
    private String path;
    private String description;
    private String prototype;
    private String className;
    private String type;
    private int mark;
    private List<Param> params;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPrototype() {
        return prototype;
    }

    public void setPrototype(String prototype) {
        this.prototype = prototype;
    }

    public void addPrototype(String prototype) {
        if (StringUtils.isNotEmpty(getPrototype())) {
            setPrototype(prototype);
        } else {
            setPrototype(getPrototype() + ", " + prototype);
        }
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (StringUtils.isNotEmpty(description)) {
            this.description = description;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getMark() {
        return mark;
    }

    public void setMark(int mark) {
        this.mark = mark;
    }

    public List<Param> getParams() {
        return params;
    }

    public void setParams(List<Param> params) {
        this.params = params;
    }

    public static class Param {
        private String key;
        private String type;
        private String description;
        private boolean required;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            if (StringUtils.isNotEmpty(description)) {
                this.description = description;
            }
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }
}
