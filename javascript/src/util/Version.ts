/// <reference path="../../declarations/node/node.d.ts" />
'use strict';

var pjson = require('../../package.json');

class Version {
    major: string;
    minor: string;
    constructor() {
        var fullVersion = pjson.version;
        var splitVersion = fullVersion.split(".");
        this.major = splitVersion[0];
        this.minor = splitVersion[1];
    }

    getMajorAndMinorVersion() {
        return this.major + "." + this.minor;
    }
}

var version = new Version();
export = version;