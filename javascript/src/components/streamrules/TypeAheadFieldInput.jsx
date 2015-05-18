/* global jsRoutes, substringMatcher */

'use strict';

var React = require('react/addons');
var Input = require('react-bootstrap').Input;
var $ = require('jquery'); // excluded and shimed

var TypeAheadFieldInput = React.createClass({
    componentDidMount() {
        if (this.refs.fieldInput) {
            var fieldInput = $(this.refs.fieldInput.getInputDOMNode());
            $.ajax({
                url: jsRoutes.controllers.api.SystemApiController.fields().url,
                success: function(data) {
                    fieldInput.typeahead({
                            hint: true,
                            highlight: true,
                            minLength: 1
                        },
                        {
                            name: 'fields',
                            displayKey: 'value',
                            source: substringMatcher(data.fields, 'value', 6)
                        });
                }
            });
        }
    },
    componentWillUnmount() {
        if (this.refs.fieldInput) {
            var fieldInput = $(this.refs.fieldInput.getInputDOMNode());
            fieldInput.typeahead('destroy');
        }
    },
    render() {
        var input = <Input ref="fieldInput" {...this.props}/>;

        return input;
    }
});

module.exports = TypeAheadFieldInput;
