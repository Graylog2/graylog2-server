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
    keyword?: string;
}

interface StackedChartSeries {
    statistical_field: string;
    field: string;
    query: string;
}

interface CreateStackedChartWidgetRequestParams {
    widgetType: string;
    renderer: string;
    interpolation: string;
    interval: string;
    rangeType: string;
    relative?: number;
    from?: string;
    to?: string;
    keyword?: string;
    series: Array<StackedChartSeries>;
}

class FieldGraphsStore {
    private renderedGraphs: Immutable.Set<string>;
    private _fieldGraphs: Immutable.Map<string, Object>;
    private _stackedGraphs: Immutable.Map<string, Immutable.Set<string>>;
    onFieldGraphCreated: (graphId: string)=>void;
    onFieldGraphsUpdated: (query: Object)=>void;
    onFieldGraphsMerged: (targetGraphId: Object)=>void;

    constructor() {
        this.renderedGraphs = Immutable.Set<string>();
        this._fieldGraphs = Immutable.Map<string, Object>(store.get("pinned-field-charts"));
        // We need a custom function to deserialize the array in the localStorage as a Set
        this._stackedGraphs = Immutable.fromJS(store.get("stacked-graphs") || {}, (key, value) => {
            var isIndexed = Immutable.Iterable.isIndexed(value);
            return isIndexed ? value.toSet() : value.toMap();
        });

        $(document).on('created.graylog.fieldgraph', (event, data) => {
            this.saveGraph(data.graphOptions['chartid'], data.graphOptions);
            this.combineStackedGraphs(data.graphOptions['chartid']);
            this.renderedGraphs.add(data.graphOptions['chartid']);
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
            this.updateStackedGraphs(data.targetGraphId, data.draggedGraphId);
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
        store.set("stacked-graphs", newStackedGraphs.toJS());
        if (typeof this.onFieldGraphsMerged === 'function') {
            this.onFieldGraphsMerged(newStackedGraphs);
        }
    }

    combineStackedGraphs(graphId: string) {
        this.renderedGraphs = this.renderedGraphs.add(graphId);
        if (this.stackedGraphs.has(graphId)) {
            var stackedGraphs = this.stackedGraphs.get(graphId);
            stackedGraphs.forEach((stackedGraphId) => {
                if (this.renderedGraphs.has(stackedGraphId)) {
                    this.stackGraphs(graphId, stackedGraphId);
                }
            });
        } else {
            this.stackedGraphs.forEach((stackedGraphs, targetGraphId) => {
                if (stackedGraphs.has(graphId) && this.renderedGraphs.has(targetGraphId)) {
                    this.stackGraphs(targetGraphId, graphId);
                }
            });
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

    updateStackedGraphs(targetGraphId: string, sourceGraphId: string) {
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

    stackGraphs(targetGraphId: string, sourceGraphId: string) {
        $(document).trigger('merge.graylog.fieldgraph', {
            targetGraphId: targetGraphId,
            sourceGraphId: sourceGraphId
        });
    }

    getFieldGraphAsCreateWidgetRequestParams(graphId: string, widgetType: string): CreateFieldChartWidgetRequestParams {
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

    private getSeriesInformation(graphOptions: Object): StackedChartSeries {
        return <StackedChartSeries> {
            query: graphOptions['query'],
            field: graphOptions['field'],
            statistical_function: graphOptions['valuetype'],
        };
    }

    getStackedGraphAsCreateWidgetRequestParams(graphId: string, widgetType: string): CreateStackedChartWidgetRequestParams {
        var graphOptions = this.fieldGraphs.get(graphId);

        if (graphOptions === undefined) {
            throw('Invalid graph ID "' + graphId + '"');
        }

        var requestParams = {
            renderer: graphOptions['renderer'],
            interpolation: graphOptions['interpolation'],
            interval: graphOptions['interval'],
            rangeType: graphOptions['rangetype']
        };

        var series = [this.getSeriesInformation(graphOptions)];

        var stackedGraphs = this.stackedGraphs.get(graphId);

        stackedGraphs.forEach((stackedGraphId) => {
            var stackedGraph = this.fieldGraphs.get(stackedGraphId);
            series.push(this.getSeriesInformation(stackedGraph));
        }, this);

        requestParams['series'] = series;

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

        return <CreateStackedChartWidgetRequestParams> requestParams;
    }
}

var fieldGraphsStore = new FieldGraphsStore();

export = fieldGraphsStore;