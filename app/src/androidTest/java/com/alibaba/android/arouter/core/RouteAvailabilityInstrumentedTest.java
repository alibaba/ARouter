package com.alibaba.android.arouter.core;

import android.app.Application;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.alibaba.android.arouter.demo.module1.testactivity.Test1Activity;
import com.alibaba.android.arouter.demo.module1.testservice.HelloServiceImpl;
import com.alibaba.android.arouter.exception.InitException;
import com.alibaba.android.arouter.facade.enums.RouteType;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.alibaba.android.arouter.facade.template.IRouteGroup;
import com.alibaba.android.arouter.launcher.ARouter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class RouteAvailabilityInstrumentedTest {

    @Before
    public void resetRouter() {
        ARouter.openDebug();
        try {
            ARouter.getInstance().destroy();
        } catch (InitException ignored) {
            // The first test in a fresh process has no initialized router to destroy.
        }

        Application application = (Application) InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getApplicationContext();
        ARouter.init(application);
    }

    @Test
    public void existingRouteLoadsItsGroupWithoutNavigating() {
        assertTrue(Warehouse.groupsIndex.containsKey("test"));
        assertFalse(Warehouse.routes.containsKey("/test/activity1"));

        assertTrue(ARouter.getInstance().hasRoute("/test/activity1"));
        assertTrue(ARouter.getInstance().hasRoute("/test/fragment"));

        assertFalse(Warehouse.groupsIndex.containsKey("test"));
        assertTrue(Warehouse.routes.containsKey("/test/activity1"));
        assertEquals(Test1Activity.class,
                Warehouse.routes.get("/test/activity1").getDestination());
    }

    @Test
    public void providerRouteCheckDoesNotInstantiateProvider() {
        assertFalse(Warehouse.providers.containsKey(HelloServiceImpl.class));

        assertTrue(ARouter.getInstance().hasRoute("/yourservicegroupname/hello"));

        assertFalse(Warehouse.providers.containsKey(HelloServiceImpl.class));
    }

    @Test
    public void invalidAndMissingRoutesReturnFalse() {
        assertFalse(ARouter.getInstance().hasRoute(null));
        assertFalse(ARouter.getInstance().hasRoute(""));
        assertFalse(ARouter.getInstance().hasRoute("test/activity1"));
        assertFalse(ARouter.getInstance().hasRoute("/test"));
        assertFalse(ARouter.getInstance().hasRoute("/missing/route"));
    }

    @Test
    public void dynamicallyRegisteredRouteBecomesAvailable() {
        assertFalse(ARouter.getInstance().hasRoute("/dynamic/probe"));

        assertTrue(ARouter.getInstance().addRouteGroup(new IRouteGroup() {
            @Override
            public void loadInto(Map<String, RouteMeta> atlas) {
                atlas.put("/dynamic/probe", RouteMeta.build(
                        RouteType.ACTIVITY,
                        Test1Activity.class,
                        "/dynamic/probe",
                        "dynamic",
                        -1,
                        Integer.MIN_VALUE
                ));
            }
        }));

        assertTrue(ARouter.getInstance().hasRoute("/dynamic/probe"));
    }
}
