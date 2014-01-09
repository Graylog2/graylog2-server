/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package models;

import controllers.routes;
import play.mvc.Call;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Startpage {

    public enum Type {
        STREAM,
        DASHBOARD
    }

    private final Type type;
    private final String id;

    public Startpage(Type type, String id) {
        this.type = type;
        this.id = id;
    }

    public Call getCall() {
        switch (type) {
            case STREAM:
                return routes.StreamSearchController.index(id, "*", "relative", 3600, "", "", "", "", 0, "", "", "");
            case DASHBOARD:
                return routes.DashboardsController.show(id);
            default:
                return null;
        }
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

}
