'use strict';

var React = require('react/addons');
var JvmHeapUsage = require('./JvmHeapUsage');

var heapUsage = document.getElementsByClassName('react-jvm-heap');
if (heapUsage) {
    for (var i = 0; i < heapUsage.length; i++) {
        var elem = heapUsage[i];
        var id = elem.getAttribute('data-node-id');
        React.render(<JvmHeapUsage nodeId={id}/>, elem);
    }
}
