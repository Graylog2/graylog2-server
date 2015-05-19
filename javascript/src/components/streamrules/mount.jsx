'use strict';

var React = require('react/addons');
var StreamRulesEditor = require('./StreamRulesEditor');
var $ = require('jquery'); // excluded and shimed

$(".react-streamrules-editor").each(function() {
    var permissions = JSON.parse(this.getAttribute('data-permissions'));
    var streamId = this.getAttribute('data-stream-id');

    var hash = window.location.hash.substring(1);
    var info = hash.split(".");
    var messageId;
    var index;
    if (info.length > 1) {
        messageId = info[0];
        index = info[1];
    }
    var component = <StreamRulesEditor permissions={permissions} streamId={streamId} messageId={messageId} index={index}/>;

    React.render(component, this);
});
