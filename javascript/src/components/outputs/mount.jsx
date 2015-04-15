'use strict';

var React = require('react/addons');
var OutputList = require('./OutputList');

var outputList = document.getElementById('react-output-list');
if (outputList) {
    var streamId = outputList.getAttribute('data-stream-id');
    var permissions = outputList.getAttribute('data-permissions');

    React.render(<OutputList streamId={streamId} permissions={permissions}/>, outputList);
}