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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import lib.BreadcrumbList;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.Version;
import org.graylog2.restclient.models.Extractor;
import org.graylog2.restclient.models.ExtractorService;
import org.graylog2.restclient.models.Input;
import org.graylog2.restclient.models.MessagesService;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.NodeService;
import org.graylog2.restclient.models.api.requests.ExtractorImportRequest;
import org.graylog2.restclient.models.api.requests.ExtractorListImportRequest;
import org.graylog2.restclient.models.api.results.MessageResult;
import play.Logger;
import play.libs.Json;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExtractorsController extends AuthenticatedController {

    @Inject
    private NodeService nodeService;
    @Inject
    private ExtractorService extractorService;
    @Inject
    private Extractor.Factory extractorFactory;
    @Inject
    private MessagesService messagesService;

    public Result manage(String nodeId, String inputId) {
        try {
            Node node = nodeService.loadNode(nodeId);
            Input input = node.getInput(inputId);

            return ok(views.html.system.inputs.extractors.manage.render(
                            currentUser(),
                            standardBreadcrumbs(node, input),
                            node,
                            input,
                            extractorService.all(node, input))
            );
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result manageGlobal(String inputId) {
        try {
            Node node = nodeService.loadMasterNode();
            Input input = node.getInput(inputId);

            return ok(views.html.system.inputs.extractors.manage.render(
                            currentUser(),
                            standardBreadcrumbs(node, input),
                            node,
                            input,
                            extractorService.all(node, input))
            );
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }
    }

    public Result newExtractor(String nodeId, String inputId, String extractorType, String field, String exampleIndex, String exampleId) {
        try {
            Node node = nodeService.loadNode(nodeId);
            Input input = node.getInput(inputId);
            MessageResult exampleMessage = messagesService.getMessage(exampleIndex, exampleId);
            String example = exampleMessage.getFields().get(field).toString();

            return ok(views.html.system.inputs.extractors.new_extractor.render(
                            currentUser(),
                            standardBreadcrumbs(node, input),
                            node,
                            input,
                            Extractor.Type.valueOf(extractorType.toUpperCase()),
                            field,
                            example)
            );
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result create(String nodeId, String inputId) {
        Map<String, String[]> form = request().body().asFormUrlEncoded();
        Extractor.Type extractorType = Extractor.Type.valueOf(form.get("extractor_type")[0].toUpperCase());

        Extractor extractor;
        try {
            Node node = nodeService.loadNode(nodeId);

            try {
                extractor = extractorFactory.forCreate(
                        Extractor.CursorStrategy.valueOf(form.get("cut_or_copy")[0].toUpperCase()),
                        form.get("title")[0],
                        form.get("source_field")[0],
                        form.get("target_field")[0],
                        extractorType,
                        currentUser(),
                        Extractor.ConditionType.valueOf(form.get("condition_type")[0].toUpperCase()),
                        form.get("condition_value")[0]
                );
            } catch (NullPointerException e) {
                Logger.error("Cannot build extractor configuration.", e);
                return badRequest();
            }

            extractor.loadConfigFromForm(extractorType, form);
            extractor.loadConvertersFromForm(form);
            extractor.create(node, node.getInput(inputId));
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not create extractor! We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }

        return redirect(controllers.routes.ExtractorsController.manage(nodeId, inputId));
    }

    public Result delete(String nodeId, String inputId, String extractorId) {
        try {
            Node node = nodeService.loadNode(nodeId);
            extractorService.delete(node, node.getInput(inputId), extractorId);

            return redirect(controllers.routes.ExtractorsController.manage(nodeId, inputId));
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not delete extractor! We expected HTTP 204, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result exportExtractors(String nodeId, String inputId) {
        try {
            Node node = nodeService.loadNode(nodeId);
            Input input = node.getInput(inputId);

            BreadcrumbList bc = standardBreadcrumbs(node, input);
            bc.addCrumb("Export", controllers.routes.ExtractorsController.exportExtractors(nodeId, inputId));

            Map<String, Object> result = Maps.newHashMap();
            List<Map<String, Object>> extractors = Lists.newArrayList();
            for (Extractor extractor : extractorService.all(node, input)) {
                extractors.add(extractor.export());
            }
            result.put("extractors", extractors);
            result.put("version", Version.VERSION.toString());

            String extractorExport = Json.stringify(Json.toJson(result));

            return ok(views.html.system.inputs.extractors.export.render(
                            currentUser(),
                            bc,
                            node,
                            input,
                            extractorExport)
            );
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result importExtractorsPage(String nodeId, String inputId) {
        try {
            Node node = nodeService.loadNode(nodeId);
            Input input = node.getInput(inputId);

            BreadcrumbList bc = standardBreadcrumbs(node, input);
            bc.addCrumb("Import", controllers.routes.ExtractorsController.importExtractorsPage(nodeId, inputId));

            return ok(views.html.system.inputs.extractors.importPage.render(
                    currentUser(),
                    bc,
                    node,
                    input
            ));
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result importExtractors(String nodeId, String inputId) {
        Map<String, String> form = flattenFormUrlEncoded(request().body().asFormUrlEncoded());

        if (!form.containsKey("extractors") || form.get("extractors").isEmpty()) {
            flash("error", "No JSON provided. Please fill out the import definition field.");
            return redirect(controllers.routes.ExtractorsController.importExtractorsPage(nodeId, inputId));
        }

        ExtractorListImportRequest elir;
        try {
            elir = Json.fromJson(Json.parse(form.get("extractors")), ExtractorListImportRequest.class);
        } catch (Exception e) {
            Logger.error("Could not read JSON.", e);
            flash("error", "Could not read JSON.");
            return redirect(controllers.routes.ExtractorsController.importExtractorsPage(nodeId, inputId));
        }

        /*
         * For future versions with breaking changes: check the "version" field in the ExtractorListImportRequest.
         *
         * Thank me later.
         */

        int successes = 0;
        for (ExtractorImportRequest importRequest : elir.extractors) {
            try {
                Node node = nodeService.loadNode(nodeId);

                Extractor.Type type = Extractor.Type.valueOf(importRequest.extractorType.toUpperCase());

                Extractor extractor = extractorFactory.forCreate(
                        Extractor.CursorStrategy.valueOf(importRequest.cursorStrategy.toUpperCase()),
                        importRequest.title,
                        importRequest.sourceField,
                        importRequest.targetField,
                        type,
                        currentUser(),
                        Extractor.ConditionType.valueOf(importRequest.conditionType.toUpperCase()),
                        importRequest.conditionValue
                );

                extractor.loadConfigFromImport(type, importRequest.extractorConfig);
                extractor.loadConvertersFromImport(importRequest.converters);
                extractor.setOrder(importRequest.order);
                extractor.create(node, node.getInput(inputId));
            } catch (Exception e) {
                Logger.error("Could not import extractor. Continuing.", e);
                continue;
            }

            successes++;
        }

        flash("success", "Successfully imported " + successes + " of " + elir.extractors.size() + " extractors.");
        return redirect(controllers.routes.ExtractorsController.manage(nodeId, inputId));
    }

    private static BreadcrumbList standardBreadcrumbs(Node node, Input input) {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", controllers.routes.SystemController.index(0));
        bc.addCrumb("Nodes", controllers.routes.NodesController.nodes());
        bc.addCrumb(node.getShortNodeId(), controllers.routes.NodesController.node(node.getNodeId()));
        bc.addCrumb("Input: " + input.getTitle(), null);
        bc.addCrumb("Extractors", controllers.routes.ExtractorsController.manage(node.getNodeId(), input.getId()));

        return bc;
    }

}
