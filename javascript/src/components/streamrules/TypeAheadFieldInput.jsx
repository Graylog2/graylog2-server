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
                success: (data) => {
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

                    if (typeof this.props.onTypeaheadLoaded === 'function') {
                        this.props.onTypeaheadLoaded();
                    }
                }
            });

            var fieldFormGroup = React.findDOMNode(this.refs.fieldInput);
            $(fieldFormGroup).on('typeahead:change typeahead:selected', (event) => {
                if (this.props.valueLink) {
                    this.props.valueLink.requestChange(event.target.value);
                }
            });
        }
    },
    componentWillUnmount() {
        if (this.refs.fieldInput) {
            var fieldInput = $(this.refs.fieldInput.getInputDOMNode());
            fieldInput.typeahead('destroy');
            var fieldFormGroup = React.findDOMNode(this.refs.fieldInput);
            $(fieldFormGroup).off('typeahead:change');
        }
    },

    render() {
        return <Input ref="fieldInput" wrapperClassName="typeahead-wrapper" {...this.props}/>;
    }
});

module.exports = TypeAheadFieldInput;
