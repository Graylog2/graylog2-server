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
    private _stackedGraphs: Immutable.Map<string, Immutable.Set<string>>;
    onFieldGraphCreated: (graphId: string)=>void;
    onFieldGraphsUpdated: (query: Object)=>void;
    onFieldGraphsMerged: (targetGraphId: Object)=>void;

    constructor() {
        this._fieldGraphs = Immutable.Map<string, Object>(store.get("pinned-field-charts"));
        this._stackedGraphs = Immutable.Map<string, Immutable.Set<string>>();

        $(document).on('created.graylog.fieldgraph', (event, data) => {
            this.saveGraph(data.graphOptions['chartid'], data.graphOptions);
            if (typeof this.onFieldGraphCreated === 'function') {
                this.onFieldGraphCreated(data.graphOptions['chartid']);
            }
        });

        $(document).on('failed.graylog.fieldgraph', (event, data) => {
            UserNotification.error(data.errorMessage, "Could not create field graph");
            this.deleteGraph(data.graphId);
        });

        $(document).on('updated.graylog.fieldgraph', (event, data) => {
            this.saveGraph(data.graphOptions['chartid'], data.graphOptions);
        });

        $(document).on('merged.graylog.fieldgraph', (event, data) => {
            this.stackGraphs(data.targetGraphId, data.draggedGraphId);
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

    get stackedGraphs(): Immutable.Map<string, Immutable.Set<string>> {
        return this._stackedGraphs;
    }

    set stackedGraphs(newStackedGraphs: Immutable.Map<string, Immutable.Set<string>>) {
        this._stackedGraphs = newStackedGraphs;
        if (typeof this.onFieldGraphsMerged === 'function') {
            this.onFieldGraphsMerged(newStackedGraphs);
        }
    }

    saveGraph(graphId: string, graphOptions: Object) {
        this.fieldGraphs = this.fieldGraphs.set(graphId, graphOptions);
    }

    deleteGraph(graphId: string): void {
        if (this.fieldGraphs.has(graphId)) {
            this.fieldGraphs = this.fieldGraphs.delete(graphId);
            if (this.stackedGraphs.has(graphId)) {
                this.deleteStackedGraphs(graphId);
            }
        }
    }

    stackGraphs(targetGraphId: string, sourceGraphId: string) {
        var newStackedGraphs: Immutable.Map<string, Immutable.Set<string>> = this.stackedGraphs;

        if (newStackedGraphs.has(targetGraphId)) {
            // targetGraphId was a stacked graph
            newStackedGraphs = newStackedGraphs.set(targetGraphId, newStackedGraphs.get(targetGraphId).add(sourceGraphId));
        } else if (newStackedGraphs.has(sourceGraphId)) {
            // draggedGraphId was a stacked graph
            var draggedMergedGraphs = newStackedGraphs.get(sourceGraphId);

            newStackedGraphs = newStackedGraphs.set(targetGraphId, draggedMergedGraphs.add(sourceGraphId));
            newStackedGraphs = newStackedGraphs.delete(sourceGraphId);
        } else {
            // None of the graphs were merged
            newStackedGraphs = newStackedGraphs.set(targetGraphId, Immutable.Set<string>().add(sourceGraphId));
        }

        this.stackedGraphs = newStackedGraphs;
    }

    deleteStackedGraphs(graphId: string) {
        var stackedGraphs = this.stackedGraphs.get(graphId);
        stackedGraphs.forEach((stackedGraphId) => this.deleteGraph(stackedGraphId));
        this.stackedGraphs = this.stackedGraphs.delete(graphId);
    }

    newFieldGraph(field: string, options?: Object) {
        var graphId = generateId();
        var givenOptions = Immutable.Map<string, Object>(options);
        var defaultOptions = Immutable.Map<string, Object>({chartid: graphId, field: field});
        this.saveGraph(graphId, defaultOptions.merge(givenOptions).toJS());
    }

    renderFieldGraph(graphOptions: Object, graphContainer: Element) {
        $(document).trigger("create.graylog.fieldgraph", {
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