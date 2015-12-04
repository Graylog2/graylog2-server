'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var StreamThroughput = require('./StreamThroughput');
var StreamComponent = require('./StreamComponent');
var $ = require('jquery');

var streamThroughput = document.getElementsByClassName('react-stream-throughput');
if (streamThroughput) {
    for (var i = 0; i < streamThroughput.length; i++) {
        var elem = streamThroughput[i];
        var id = elem.getAttribute('data-stream-id');
        ReactDOM.render(<StreamThroughput streamId={id}/>, elem);
    }
}

$(".react-stream-component").each(function() {
    var permissions = JSON.parse(this.getAttribute('data-permissions'));
    var username = this.getAttribute('data-user-name');
    var component = <StreamComponent permissions={permissions} username={username}/>;

    ReactDOM.render(component, this);
});
