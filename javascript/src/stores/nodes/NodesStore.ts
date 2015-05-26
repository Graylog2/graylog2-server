'use strict';

declare var $: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

interface Node {
    hostname: string;
    node_id: string;
    short_node_id: string;
}

class NodesStore {
    public static instance: NodesStore = null;

    private NODES_URL: string = URLUtils.appPrefixed('/a/system/nodes');
    private nodes: any = {};
    private isLoading: boolean = false;

    load() {
        // Do not try to load the nodes again if we are currently loading.
        if (this.isLoading) {
            return;
        }

        this.isLoading = true;

        var failCallback = (jqXHR, textStatus, errorThrown) => {
            this.isLoading = false;
            UserNotification.error("Loading nodes failed with status: " + errorThrown,
                "Could not load nodes");
        };

        $.getJSON(this.NODES_URL, (nodes: Array<Node>) => {
            nodes.forEach((node) => {
                this.nodes[node.node_id] = node;
            });
            this.isLoading = false;
        }).fail(failCallback);
    }

    get(nodeId) {
        return this.nodes[nodeId];
    }
}

export = NodesStore;