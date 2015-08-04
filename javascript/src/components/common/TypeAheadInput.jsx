/* global substringMatcher */

'use strict';

var React = require('react');
var Input = require('react-bootstrap').Input;
var $ = require('jquery');

var TypeAheadInput = React.createClass({

    componentDidMount() {
        this.fieldInput = this.refs.fieldInput.getInputDOMNode();
        this.fieldFormGroup = React.findDOMNode(this.refs.fieldInput);

        var $fieldInput = $(this.fieldInput);

        // this.props.suggestions:
        // [ "some string", "otherstring" ]
        $fieldInput.typeahead({
                hint: true,
                highlight: true,
                minLength: 1
            },
            {
                name: 'dataset-name',
                displayKey: this.props.displayKey,
                source: substringMatcher(this.props.suggestions, this.props.displayKey, 6),
                templates: {
                    suggestion: (value) => `<div><strong>${ this.props.suggestionText }</strong> ${ value.value }</div>`
                }
            });

        if (typeof this.props.onTypeaheadLoaded === 'function') {
            this.props.onTypeaheadLoaded();
            $fieldInput.typeahead('close');
        }

        $(this.fieldFormGroup).on('typeahead:select typeahead:autocomplete', (event, suggestion) => {
            this.props.onSuggestionSelected(event, suggestion);
        });
    },
    componentWillUnmount() {
        $(this.fieldInput).typeahead('destroy');
        $(this.fieldFormGroup).off('typeahead:select typeahead:autocomplete');
    },

    componentWillReceiveProps(newProps) {
        // TODO: Update typeahead dataset
    },

    getValue() {
        return $(this.fieldInput).typeahead('val');
    },
    clear() {
        $(this.fieldInput).typeahead('val', '');
    },
    render() {
        return <Input type="text" ref="fieldInput"
                      wrapperClassName="typeahead-wrapper"
                      label={this.props.label}
                      onKeyPress={this.props.onKeyPress}/>;
    }
});

module.exports = TypeAheadInput;
