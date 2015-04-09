'use strict';

var React = require('react/addons');
var JvmHeapUsage = require('./JvmHeapUsage');
var BufferUsage = require('./BufferUsage');

var heapUsage = document.getElementsByClassName('react-jvm-heap');
if (heapUsage) {
    for (var i = 0; i < heapUsage.length; i++) {
        var elem = heapUsage[i];
        var id = elem.getAttribute('data-node-id');
        React.render(<JvmHeapUsage nodeId={id}/>, elem);
    }
}

var buffers = document.getElementsByClassName('react-buffer-usage');
if (buffers) {
    for (var i = 0; i < buffers.length; i++) {
        var elem = buffers[i];
        var id = elem.getAttribute('data-node-id');
        var title = elem.getAttribute('data-title');
        var bufferType = elem.getAttribute('data-buffer-type');
        React.render(<BufferUsage nodeId={id} title={title} bufferType={bufferType}/>, elem);
    }
}