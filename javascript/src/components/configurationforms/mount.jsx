'use strict';

var React = require('react');
var $ = require('jquery');
var ConfigurationForm = require('./ConfigurationForm');

$('.react-configuration-form').each(function() {
    var title = this.getAttribute('data-title');
    var typeName = this.getAttribute('data-type-name');
    var elementName = this.getAttribute('data-element-name');
    var formTarget = this.getAttribute('data-form-target');
    var formId = this.getAttribute('data-form-id');
    React.render(<ConfigurationForm title={title} typeName={typeName} elementName={elementName} formTarget={formTarget} formId={formId}/>, this);
});
