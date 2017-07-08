package com.alibaba.android.arouter.base;

import com.alibaba.android.arouter.facade.template.IRouteGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by metide on 2017/7/7.
 */

public class RouteGroupMap extends HashMap<String,List<Class<? extends IRouteGroup>>> {


    public void put(String k, Class<? extends IRouteGroup> clz){
        if(k != null && clz != null){

            List<Class<? extends IRouteGroup>> temp;

            if(containsKey(k)){
                temp = get(k);

                if(!temp.contains(clz)){
                    temp.add(clz);
                }
            }else{
                temp = new ArrayList<>();
                temp.add(clz);
            }

            put(k, temp);
        }
    }
}
