'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var ExtractorExampleMessage = require('./ExtractorExampleMessage');

var extractorExampleMessage = document.getElementById('react-extractor-example-message');
if (extractorExampleMessage) {
    var example = extractorExampleMessage.getAttribute('data-example');
    var field = extractorExampleMessage.getAttribute('data-field');
    ReactDOM.render(<ExtractorExampleMessage field={field} example={example}/>, extractorExampleMessage);
}
