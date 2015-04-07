'use strict';

var React = require('react/addons');
var AgentList = require('./AgentList');

var agentList = document.getElementById('react-agent-list');
if (agentList) {
    React.render(<AgentList />, agentList);
}
