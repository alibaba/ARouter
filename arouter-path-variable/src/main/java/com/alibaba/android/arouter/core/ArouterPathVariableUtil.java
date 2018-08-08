package com.alibaba.android.arouter.core;

import android.content.Context;
import android.net.Uri;
import com.alibaba.android.arouter.exception.NoRouteFoundException;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.alibaba.android.arouter.pathvariable.RequestMapping;
import com.alibaba.android.arouter.pathvariable.RequestMappingInfo;
import com.alibaba.android.arouter.utils.TextUtils;

/**
 * Created by tong on 2018/2/28.
 */
class ArouterPathVariableUtil {
    private static volatile int groupsIndexSize = 0;
    private static List<String> groupsIndexList = new ArrayList<>();

    private static RequestMapping mapping = new RequestMapping();

    synchronized static void init(Context context) {
        groupsIndexSize = Warehouse.groupsIndex.size();
        groupsIndexList.addAll(Warehouse.groupsIndex.keySet());

        //防止某些groupsIndex已经被加载
        syncLoadedRouters();
    }

    static Uri forUri(Uri uri) {
        String group = extractGroup(uri.getPath());
        if (Warehouse.groupsIndex.containsKey(group)) {
            try {
                LogisticsCenter.completion(new Postcard(uri.getPath(),group));
            } catch (NoRouteFoundException ignored) {}
        }
        syncRoutes();
        try {
            RequestMappingInfo mappingInfo = mapping.lookupHandlerMethod(uri.getPath());
            if (mappingInfo != null) {
                String pattern = mappingInfo.getName();
                Map<String, String> params = mapping.getPathMatcher().extractUriTemplateVariables(mappingInfo.getPatternsCondition().getFirstPattern(),uri.getPath());
                Map<String, String> resultMap = new HashMap<>(TextUtils.splitQueryParameters(uri,false));
                resultMap.putAll(params);
                String newQueryString = TextUtils.joinQueryString(resultMap);
                return uri.buildUpon().encodedPath(pattern).encodedQuery(newQueryString).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    private static void syncLoadedRouters() {
        for (Map.Entry<String, RouteMeta> entry : Warehouse.routes.entrySet()) {
            if (isPathVariablePattern(entry.getKey())) {
                mapping.registerMapping(RequestMappingInfo.paths(entry.getKey()).mappingName(entry.getKey()).build());
            }
        }
    }

    private static boolean isPathVariablePattern(String pattern) {
        return pattern.contains("/{") && pattern.contains("}");
    }

    private static synchronized void syncRoutes() {
        int curGroupsIndexSize;
        if (groupsIndexSize != (curGroupsIndexSize = Warehouse.groupsIndex.size())) {
            ArrayList<String> deletedGroups = new ArrayList<>();
            deletedGroups.addAll(groupsIndexList);
            deletedGroups.removeAll(Warehouse.groupsIndex.keySet());

            for (Map.Entry<String, RouteMeta> entry : Warehouse.routes.entrySet()) {
                for (String group : deletedGroups) {
                    if (entry.getKey().startsWith("/" + group)) {
                        if (isPathVariablePattern(entry.getKey())) {
                            mapping.registerMapping(RequestMappingInfo.paths(entry.getKey()).mappingName(entry.getKey()).build());
                        }
                        break;
                    }
                }
            }
            groupsIndexList.removeAll(deletedGroups);
            groupsIndexSize = curGroupsIndexSize;
        }
    }

    /**
     * Extract the default group from path.
     */
    private static String extractGroup(String path) {
        try {
            int index = path.indexOf("/", 1);
            if (index == -1) {
                index = path.length();
            }
            return path.substring(1, index);
        } catch (Exception e) {
            return "";
        }
    }
}
