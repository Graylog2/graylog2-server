/// <reference path='../../../node_modules/immutable/dist/immutable.d.ts'/>

'use strict';

declare
var $: any;
declare
var store: any;
declare
var generateId: ()=>string;

import Immutable = require('immutable');

import UserNotification = require("../../util/UserNotification");

interface CreateFieldChartWidgetRequestParams {
    widgetType: string;
    valuetype: string;
    renderer: string;
    interpolation: string;
    interval: string;
    field: string;
    query: string;
    rangeType: string;
    relative?: number;
    from?: string;
    to?: string;
    keyword: string;
}

class FieldGraphsStore {
    private _fieldGraphs: Immutable.Map<string, Object>;
    onFieldGraphsUpdated: (query: Object)=>void;

    constructor() {
        this._fieldGraphs = Immutable.Map<string, Object>(store.get("pinned-field-charts"));
        $(document).on('created.graylog.fieldgraph', (event, data) => {
            this.saveGraph(data.graphOptions['chartid'], data.graphOptions);
        });
        $(document).on('failed.graylog.fieldgraph', (event, data) => {
            UserNotification.error(data.errorMessage, "Could not create field graph");
            this.deleteGraph(data.graphId);
        });
        $(document).on('updated.graylog.fieldgraph', (event, data) => {
            this.saveGraph(data.graphOptions['chartid'], data.graphOptions);
        });
    }

    get fieldGraphs(): Immutable.Map<string, Object> {
        return this._fieldGraphs;
    }

    set fieldGraphs(newFieldGraphs: Immutable.Map<string, Object>) {
        this._fieldGraphs = newFieldGraphs;
        store.set("pinned-field-charts", newFieldGraphs.toJS());
        if (typeof this.onFieldGraphsUpdated === 'function') {
            this.onFieldGraphsUpdated(newFieldGraphs);
        }
    }

    saveGraph(graphId: string, graphOptions: Object) {
        this.fieldGraphs = this.fieldGraphs.set(graphId, graphOptions);
    }

    deleteGraph(graphId: string): void {
        if (this.fieldGraphs.has(graphId)) {
            this.fieldGraphs = this.fieldGraphs.delete(graphId);
        }
    }

    newFieldGraph(field: string, options?: Object) {
        var graphId = generateId();
        var givenOptions = Immutable.Map<string, Object>(options);
        var defaultOptions = Immutable.Map<string, Object>({chartid: graphId, field: field});
        this.saveGraph(graphId, defaultOptions.merge(givenOptions).toJS());
    }

    renderFieldGraph(graphOptions: Object, graphContainer: Element) {
        $(document).trigger("create.graylog.fieldgraph",
            {
                options: graphOptions,
                container: graphContainer
            });
    }

    getGraphOptionsAsCreateWidgetRequestParams(graphId: string, widgetType: string): CreateFieldChartWidgetRequestParams {
        var graphOptions = this.fieldGraphs.get(graphId);

        if (graphOptions === undefined) {
            throw('Invalid graph ID "' + graphId + '"');
        }

        var requestParams = {
            valuetype: graphOptions['valuetype'],
            renderer: graphOptions['renderer'],
            interpolation: graphOptions['interpolation'],
            interval: graphOptions['interval'],
            field: graphOptions['field'],
            query: graphOptions['query'],
            rangeType: graphOptions['rangetype']
        };

        switch (graphOptions['rangetype']) {
            case "relative":
                requestParams['relative'] = graphOptions['range']['relative'];
                break;
            case "absolute":
                requestParams['from'] = graphOptions['range']['from'];
                requestParams['to'] = graphOptions['range']['to'];
                break;
            case "keyword":
                requestParams['keyword'] = graphOptions['range']['keyword'];
                break;
        }

        return <CreateFieldChartWidgetRequestParams> requestParams;
    }
}

var fieldGraphsStore = new FieldGraphsStore();

export = fieldGraphsStore;