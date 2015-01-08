'use strict';

var React = require('react/addons');
var GrokPatterns = require('./GrokPatterns');

var grokPatterns = document.getElementById('react-grok-patterns');
if (grokPatterns) {
    React.render(<GrokPatterns />, grokPatterns);
}
