'use strict';

declare var $: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

interface NodeDetails {
    operating_system: string;
}
interface Agent {
    id: string;
    node_id: string;
    node_details: NodeDetails;
    last_seen: number;
}

var AgentsStore = {
    URL: URLUtils.appPrefixed('/a/system/agents'),

    load(callback: (agents: Array<Agent>) => void) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Loading agents failed with status: " + errorThrown,
                "Could not load agents");
        };
        $.getJSON(this.URL, (agents: Array<Agent>) => {
            callback(agents);
        }).fail(failCallback);
    }
};

export = AgentsStore;