import $ = require('jquery');
const UserNotification = require('util/UserNotification');
const URLUtils = require('util/URLUtils');
const StringUtils = require('util/StringUtils');
const fetch = require('logic/rest/FetchProvider').default;

interface Source {
    name: string;
    message_count: number;
    percentage: number;
}

const processSourcesData = (sources: Object): Array<Source> => {
    let total = 0;
    let sourcesArray = [];
    $.each(sources, (name, count) => {
        total += count;
        sourcesArray.push({name: StringUtils.escapeHTML(name), message_count: count})
    });
    sourcesArray.forEach((d) => {
        d.percentage = d.message_count / total * 100;
    });
    return sourcesArray;
};

const SourcesStore = {
    SOURCES_URL: '/sources',

    loadSources(range: number, callback: (sources: Array<Source>) => void) {
        let url = URLUtils.qualifyUrl(this.SOURCES_URL);
        if (typeof range !== 'undefined') {
            url += "?range=" + range;
        }
        fetch('GET', url)
            .then(response => {
                var sources = processSourcesData(response.sources);
                callback(sources);
            })
            .catch((errorThrown) => {
                UserNotification.error("Loading of sources data failed with status: " + errorThrown + ". Try reloading the page.",
                    "Could not load sources data");
            });
    }
};

export = SourcesStore;
