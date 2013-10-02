/*
 * Copyright 2013 TORCH UG
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
 */
package controllers;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import lib.BreadcrumbList;
import lib.ServerNodes;
import models.InternalLogger;
import models.Node;
import models.NodeService;
import play.mvc.Result;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class LoggingController extends AuthenticatedController {

    @Inject
    private NodeService nodeService;
    @Inject
    private ServerNodes serverNodes;

    public Result index() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Logging", routes.LoggingController.index());

        Map<Node, List<InternalLogger>> loggers = Maps.newHashMap();
        for (Node node : serverNodes.all()) {
            loggers.put(node, node.allLoggers());
        }

        return ok(views.html.system.logging.index.render(currentUser(), bc, loggers));
    }

}