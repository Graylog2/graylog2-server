'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var GrokPatterns = require('./GrokPatterns');

var grokPatterns = document.getElementById('react-grok-patterns');
if (grokPatterns) {
    ReactDOM.render(<GrokPatterns />, grokPatterns);
}
