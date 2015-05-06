/// <reference path='../../../node_modules/immutable/dist/immutable.d.ts'/>

'use strict';

declare var $: any;
declare var store: any;
declare var generateId: ()=>string;

import Immutable = require('immutable');

class FieldGraphsStore {
    private _fieldGraphs: Immutable.Map<string, Object>;
    onFieldGraphsUpdated: (query: Object)=>void;

    constructor() {
        this._fieldGraphs = Immutable.Map<string, Object>(store.get("pinned-field-charts"));
        $(document).on("created.graylog.fieldgraph", (event, data) => {
            this.saveGraph(data.graphOptions['chartid'], data.graphOptions);
        });
        $(document).on("updated.graylog.fieldgraph", (event, data) => {
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

    newFieldGraph(field: string) {
        var graphId = generateId();
        this.saveGraph(graphId, { chartid: graphId, field: field });
    }

    renderFieldGraph(graphId: string, graphOptions: Object, graphContainer: Element) {
        $(document).trigger("create.graylog.fieldgraph",
            {
                options: graphOptions,
                container: graphContainer
            });
    }
}

var fieldGraphsStore = new FieldGraphsStore();

export = fieldGraphsStore;