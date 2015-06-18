'use strict';

var React = require('react');
var ExtractorExampleMessage = require('./ExtractorExampleMessage');

var extractorExampleMessage = document.getElementById('react-extractor-example-message');
if (extractorExampleMessage) {
    var example = extractorExampleMessage.getAttribute('data-example');
    var field = extractorExampleMessage.getAttribute('data-field');
    React.render(<ExtractorExampleMessage field={field} example={example}/>, extractorExampleMessage);
}
