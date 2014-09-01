'use strict';

function mixin(_sub, _super) {
    for (var p in _super.prototype) {
        if (_super.prototype.hasOwnProperty(p) && p !== 'constructor') {
            _sub.prototype[p] = _super.prototype[p];
        }
    }
}
function mergeInto(to, from) {
    for (var p in from) {
        if (from.hasOwnProperty(p)) {
            to[p] = from[p];
        }
    }
}
function _extends(_sub, _super) {
    _sub.prototype = Object.create(_super.prototype);
    _sub.prototype.constructor = _sub;
}

exports._extends = _extends;
exports.mixin = mixin;
exports.mergeInto = mergeInto;
