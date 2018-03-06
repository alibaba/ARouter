package com.alibaba.android.arouter.pathvariable;

import android.content.Context;
import android.net.Uri;
import com.alibaba.android.arouter.core.LogisticsCenter;
import com.alibaba.android.arouter.core.WarehouseProxy;
import com.alibaba.android.arouter.exception.NoRouteFoundException;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.alibaba.android.arouter.pathvariable.mapping.RequestMapping;
import com.alibaba.android.arouter.pathvariable.mapping.RequestMappingInfo;

/**
 * Created by tong on 2018/2/28.
 */
public class ArouterPathVariableUtil {
    private static volatile int groupsIndexSize = 0;
    private static List<String> groupsIndexList = new ArrayList<>();

    private static RequestMapping mapping = new RequestMapping();

    public synchronized static void init(Context context) {
        groupsIndexSize = WarehouseProxy.getGroupsIndex().size();
        groupsIndexList.addAll(WarehouseProxy.getGroupsIndex().keySet());
    }

    public static Uri forUri(Uri uri) {
        String group = extractGroup(uri.getPath());
        if (WarehouseProxy.getGroupsIndex().containsKey(group)) {
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
                Map<String, String> resultMap = new HashMap<>(splitQueryParameters(uri));
                resultMap.putAll(params);
                String newQueryString = joinQueryString(resultMap);
                return uri.buildUpon().encodedPath(pattern).encodedQuery(newQueryString).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    private static synchronized void syncRoutes() {
        int curGroupsIndexSize;
        if (groupsIndexSize != (curGroupsIndexSize = WarehouseProxy.getGroupsIndex().size())) {
            ArrayList<String> deletedGroups = new ArrayList<>();
            deletedGroups.addAll(groupsIndexList);
            deletedGroups.removeAll(WarehouseProxy.getGroupsIndex().keySet());

            Map<String, RouteMeta> routes = new HashMap<>(WarehouseProxy.getRoutes());
            for (Map.Entry<String, RouteMeta> entry : routes.entrySet()) {
                for (String group : deletedGroups) {
                    if (entry.getKey().startsWith("/" + group)) {
                        if (entry.getKey().contains("/{") && entry.getKey().contains("}")) {
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
            String defaultGroup = path.substring(1, index);
            return defaultGroup;
        } catch (Exception e) {
            return "";
        }
    }

    private static String joinQueryString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Split query parameters
     * @param rawUri raw uri
     * @return map with params
     */
    private static Map<String, String> splitQueryParameters(Uri rawUri) {
        String query = rawUri.getQuery();

        if (query == null) {
            return Collections.emptyMap();
        }

        Map<String, String> paramMap = new LinkedHashMap<>();
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            int end = (next == -1) ? query.length() : next;

            int separator = query.indexOf('=', start);
            if (separator > end || separator == -1) {
                separator = end;
            }

            String name = query.substring(start, separator);

            if (!android.text.TextUtils.isEmpty(name)) {
                String value = (separator == end ? "" : query.substring(separator + 1, end));
                paramMap.put(Uri.decode(name), Uri.decode(value));
            }

            // Move start to end of name.
            start = end + 1;
        } while (start < query.length());

        return Collections.unmodifiableMap(paramMap);
    }
}
