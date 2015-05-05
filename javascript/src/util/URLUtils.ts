/// <reference path="../../declarations/node/node.d.ts" />

'use strict';

var Qs = require('qs');

declare var gl2AppPathPrefix: string;

var URLUtils = {
    appPrefixed(url) {
        return gl2AppPathPrefix + url;
    },
    openLink(url, newWindow) {
        if (newWindow) {
            window.open(url);
        } else {
            window.location = url;
        }
    },
    getParsedSearch(location) {
        var search = {};
        var query = location.search;
        if (query) {
            if (query.indexOf("?") === 0 && query.length > 1) {
                query = query.substr(1, query.length - 1);
                search = Qs.parse(query);
            }
        }

        return search;
    },
    getParsedHash(location) {
        var result = {};
        var hash = location.hash;
        if (hash) {
            if (hash.indexOf("#") === 0 && hash.length > 1) {
                hash = hash.substr(1, hash.length - 1);
                result = Qs.parse(hash);
            }
        }
        return result;
    },
    replaceHashParam(name, newValue) {
        var origHash = URLUtils.getParsedHash(window.location);
        origHash[name] = newValue;
        window.location.hash = Qs.stringify(origHash);
    }
};

export = URLUtils;