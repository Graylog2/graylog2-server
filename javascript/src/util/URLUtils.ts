'use strict';

declare var gl2AppPathPrefix: string;

var URLUtils = {
    appPrefixed(url) {
        return gl2AppPathPrefix + url;
    }
};

export = URLUtils;