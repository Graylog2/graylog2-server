'use strict';

var React = require('react/addons');
var StreamThroughput = require('./StreamThroughput');

var streamThroughput = document.getElementsByClassName('react-stream-throughput');
if (streamThroughput) {
    for (var i = 0; i < streamThroughput.length; i++) {
        var elem = streamThroughput[i];
        var id = elem.getAttribute('data-stream-id');
        React.render(<StreamThroughput streamId={id}/>, elem);
    }
}
