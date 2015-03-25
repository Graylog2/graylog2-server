'use strict';

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
    }
};

export = URLUtils;