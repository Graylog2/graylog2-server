'use strict';

declare var $: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

interface NodeDetails {
    operating_system: string;
}
interface Collector {
    id: string;
    node_id: string;
    node_details: NodeDetails;
    last_seen: number;
    collector_version: String;
}

var CollectorsStore = {
    URL: URLUtils.appPrefixed('/a/system/collectors'),

    load(callback: (collectors: Array<Collector>) => void) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Loading collectors failed with status: " + errorThrown,
                "Could not load collectors");
        };
        $.getJSON(this.URL, (collectors: Array<Collector>) => {
            callback(collectors);
        }).fail(failCallback);
    }
};

export = CollectorsStore;