/* global jsRoutes, substringMatcher */

'use strict';

var React = require('react/addons');
var Immutable = require('immutable');
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
                        fieldInput.typeahead('close');
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
            $(fieldFormGroup).off('typeahead:change typeahead:selected');
        }
    },

    _getFilteredProps() {
        var props = Immutable.fromJS(this.props);

        if (props.has('valueLink')) {
            props = props.delete('valueLink');
        }

        return props.toJS();
    },

    render() {
        return <Input ref="fieldInput"
                      wrapperClassName="typeahead-wrapper"
                      defaultValue={this.props.valueLink.value}
                      {...this._getFilteredProps()}/>;
    }
});

module.exports = TypeAheadFieldInput;
