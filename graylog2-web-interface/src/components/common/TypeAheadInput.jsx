import React, { PropTypes } from 'react';
import ReactDOM from 'react-dom';
import { Input } from 'components/bootstrap';
import UniversalSearch from 'logic/search/UniversalSearch';
import $ from 'jquery';
// eslint-disable-next-line no-unused-vars
import Typeahead from 'typeahead.js';

const TypeAheadInput = React.createClass({
  propTypes: {
    label: PropTypes.string.isRequired,
    onKeyPress: PropTypes.func,
    displayKey: PropTypes.string,
    suggestions: PropTypes.array, // [ "some string", "otherstring" ]
    suggestionText: PropTypes.string,
    onTypeaheadLoaded: PropTypes.func,
    onSuggestionSelected: PropTypes.func,
  },

  componentDidMount() {
    this._updateTypeahead(this.props);
  },
  componentWillReceiveProps(newProps) {
    this._destroyTypeahead();
    this._updateTypeahead(newProps);
  },
  componentWillUnmount() {
    this._destroyTypeahead();
  },

  getValue() {
    return $(this.fieldInput).typeahead('val');
  },
  clear() {
    $(this.fieldInput).typeahead('val', '');
  },
  _destroyTypeahead() {
    $(this.fieldInput).typeahead('destroy');
    $(this.fieldFormGroup).off('typeahead:select typeahead:autocomplete');
  },
  _updateTypeahead(props) {
    this.fieldInput = this.refs.fieldInput.getInputDOMNode();
    this.fieldFormGroup = ReactDOM.findDOMNode(this.refs.fieldInput);

    const $fieldInput = $(this.fieldInput);

    $fieldInput.typeahead({
      hint: true,
      highlight: true,
      minLength: 1,
    },
      {
        name: 'dataset-name',
        displayKey: props.displayKey,
        source: UniversalSearch.substringMatcher(props.suggestions, props.displayKey, 6),
        templates: {
          suggestion: (value) => {
            if (props.suggestionText) {
              return `<div><strong>${props.suggestionText}</strong> ${value[props.displayKey]}</div>`;
            }
            return `<div>${value.value}</div>`;
          },
        },
      });

    if (typeof props.onTypeaheadLoaded === 'function') {
      props.onTypeaheadLoaded();
      $fieldInput.typeahead('close');
    }

    $(this.fieldFormGroup).on('typeahead:select typeahead:autocomplete', (event, suggestion) => {
      if (props.onSuggestionSelected) {
        props.onSuggestionSelected(event, suggestion);
      }
    });
  },
  render() {
    return (<Input type="text" ref="fieldInput"
                   wrapperClassName="typeahead-wrapper"
                   label={this.props.label}
                   onKeyPress={this.props.onKeyPress} />);
  },
});

export default TypeAheadInput;
