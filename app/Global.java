/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */

import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.api.mvc.EssentialFilter;
import play.api.mvc.Handler;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.io.File;
import java.lang.reflect.Method;

/**
 * This class is simply delegating to the real class {@see lib.Global},
 * to avoid having to configure the exact name in the application.conf
 * which is lost when a user overrides the config file location with -Dconfig.file etc.
 */
@SuppressWarnings("unused")
public class Global extends GlobalSettings {
    private final lib.Global global = new lib.Global();


    @Override
    public void onStart(Application app) {
        global.onStart(app);
    }

    @Override
    public void onStop(Application app) {
        global.onStop(app);
    }

    @Override
    public Configuration onLoadConfig(Configuration configuration, File file, ClassLoader classLoader) {
        return global.onLoadConfig(configuration, file, classLoader);
    }

    @Override
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        return global.getControllerInstance(controllerClass);
    }

    @Override
    public Handler onRouteRequest(Http.RequestHeader request) {
        return global.onRouteRequest(request);
    }

    @Override
    public Action onRequest(Http.Request request, Method actionMethod) {
        return global.onRequest(request, actionMethod);
    }

    @Override
    public F.Promise<Result> onHandlerNotFound(Http.RequestHeader request) {
        return global.onHandlerNotFound(request);
    }

    @Override
    public F.Promise<Result> onError(Http.RequestHeader request, Throwable t) {
        return global.onError(request, t);
    }

    @Override
    public F.Promise<Result> onBadRequest(Http.RequestHeader request, String error) {
        return global.onBadRequest(request, error);
    }

    @Override
    public void beforeStart(Application app) {
        global.beforeStart(app);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends EssentialFilter> Class<T>[] filters() {
        return global.filters();
    }
}
