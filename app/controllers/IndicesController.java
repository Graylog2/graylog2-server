/**
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
package controllers;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import lib.BreadcrumbList;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.Tools;
import org.graylog2.restclient.models.ClusterService;
import org.graylog2.restclient.models.ESClusterHealth;
import org.graylog2.restclient.models.IndexService;
import org.graylog2.restclient.models.api.responses.system.indices.ClosedIndicesResponse;
import org.graylog2.restclient.models.api.responses.system.indices.DeflectorConfigResponse;
import org.graylog2.restclient.models.api.responses.system.indices.DeflectorInformationResponse;
import play.mvc.Result;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

public class IndicesController extends AuthenticatedController {

    @Inject
    private ClusterService clusterService;

    @Inject
    private IndexService indexService;

    public Result index() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Indices", routes.IndicesController.index());

        try {
            ESClusterHealth clusterHealth = clusterService.getESClusterHealth();
            DeflectorInformationResponse deflector;
            DeflectorConfigResponse deflectorConfig;
            ClosedIndicesResponse closedIndices;
            ClosedIndicesResponse reopenedIndices;
            try {
                deflector = indexService.getDeflectorInfo();
                deflectorConfig = indexService.getDeflectorConfig();
                closedIndices = indexService.getClosedIndices();
                reopenedIndices = indexService.getReopenedIndices();
            } catch (APIException e) {
                if (Tools.rootCause(e) instanceof TimeoutException) {
                    return ok(views.html.system.indices.index_cluster_unavailable.render(
                            currentUser(),
                            bc,
                            clusterHealth
                    ));
                }
                throw e;
            }

            final HashSet<String> reopenedSet = Sets.newHashSet(reopenedIndices.indices);

            return ok(views.html.system.indices.index.render(
                    currentUser(),
                    bc,
                    indexService.all(),
                    closedIndices.indices,
                    reopenedSet,
                    clusterHealth,
                    deflector.currentTarget,
                    deflectorConfig
            ));
        } catch (APIException e) {
            String message = "Could not get indices. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result failures(Integer page) {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Indices", routes.IndicesController.index());
        bc.addCrumb("Failures", routes.IndicesController.failures(0));

        try {
            return ok(views.html.system.indices.failures.render(
                    currentUser(),
                    bc,
                    clusterService.getIndexerFailures(0,0).total,
                    page
            ));
        } catch (APIException e) {
            String message = "Could not get indexer failures. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result closeIndex(String index) {
        try {
            indexService.close(index);
            return redirect(routes.IndicesController.index());
        } catch (APIException e) {
            String message = "Could not close index. We expected HTTP 204, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result reopenIndex(String index) {
        try {
            indexService.reopen(index);
            return redirect(routes.IndicesController.index());
        } catch (APIException e) {
            String message = "Could not reopen index. We expected HTTP 204, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result deleteIndex(String index) {
        try {
            indexService.delete(index);
            return redirect(routes.IndicesController.index());
        } catch (APIException e) {
            String message = "Could not delete index. We expected HTTP 204, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result reCalculateRanges() {
        try {
            indexService.recalculateRanges();
            return redirect(routes.SystemController.index(0));
        } catch (APIException e) {
            String message = "Could not re-calculation trigger system job. We expected HTTP 202, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result cycleDeflector() {
        try {
            indexService.cycleDeflector();
            return redirect(routes.IndicesController.index());
        } catch (APIException e) {
            String message = "Could not cycle deflector. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

}
