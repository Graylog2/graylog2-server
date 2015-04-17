'use strict';

var React = require('react/addons');
var OutputList = require('./OutputList');
var CreateOutputDropdown = require('./CreateOutputDropdown');
var $ = require('jquery'); // excluded and shimed

var outputList = document.getElementById('react-output-list');
if (outputList) {
    var streamId = outputList.getAttribute('data-stream-id');
    var permissions = outputList.getAttribute('data-permissions');

    React.render(<OutputList streamId={streamId} permissions={permissions}/>, outputList);
}

$("#react-create-output-dropdown").each(function() {
    React.render(<CreateOutputDropdown />, this);
});