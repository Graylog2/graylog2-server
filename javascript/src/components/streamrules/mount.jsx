'use strict';

var React = require('react/addons');
var StreamRulesComponent = require('./StreamRulesComponent');
var StreamRulesEditor = require('./StreamRulesEditor');
var $ = require('jquery'); // excluded and shimed

$(".react-streamrules-editor").each(function() {
    var permissions = JSON.parse(this.getAttribute('data-permissions'));
    var streamId = this.getAttribute('data-stream-id');

    var hash = window.location.hash.substring(1);
    var info = hash.split(".");
    if (info.length > 1) {
        var messageId = info[0];
        var index = info[1];
    }
    var component = <StreamRulesEditor permissions={permissions} streamId={streamId} messageId={messageId} index={index}/>;

    React.render(component, this);
});
