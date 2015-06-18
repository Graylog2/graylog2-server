'use strict';

var React = require('react');

var FieldHelpers = {
    hasAttribute: (ary, attribute) => {
        return ary.indexOf(attribute) > -1;
    },
    optionalMarker: (field) => {
        return field.is_optional ? <span className="configuration-field-optional">(optional)</span> : "";
    }
};

module.exports = FieldHelpers;
