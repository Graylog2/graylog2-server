/// <reference path="../../declarations/node/node.d.ts" />
'use strict';

var pjson = require('../../package.json');

class Version {
    full: string
    major: string;
    minor: string;
    constructor() {
        this.full = pjson.version;
        var splitVersion = this.full.split(".");
        this.major = splitVersion[0];
        this.minor = splitVersion[1];
    }

    getMajorAndMinorVersion() {
        return this.major + "." + this.minor;
    }

    getFullVersion() {
        return this.full;
    }
}

var version = new Version();
export = version;
