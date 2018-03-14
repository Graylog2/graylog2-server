import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import { Input } from 'components/bootstrap';
import UniversalSearch from 'logic/search/UniversalSearch';
import $ from 'jquery';
// eslint-disable-next-line no-unused-vars
import Typeahead from 'typeahead.js';

/**
 * Component that renders a field input with auto-completion capabilities.
 *
 * **Note** There are a few quirks around this component and it will be
 * refactored soon.
 */
class TypeAheadInput extends React.Component {
  static propTypes = {
    /** ID to use in the input field. */
    id: PropTypes.string.isRequired,
    /** Label to use for the input field. */
    label: PropTypes.string.isRequired,
    /**
     * Function that will be called when a new key is pressed in the
     * input field. The function will be called with the event generated
     * by react for that input.
     */
    onKeyPress: PropTypes.func,
    /** Object key where to store auto-completion result. */
    displayKey: PropTypes.string,
    /**
     * Array of strings providing auto-completion.
     * E.g. `[ "some string", "otherstring" ]`
     */
    suggestions: PropTypes.array,
    /** Text to display next to the auto-completion suggestion. */
    suggestionText: PropTypes.string,
    /**
     * Function that will be called once typeahead is loaded and ready
     * to operate. The function will be called with no arguments.
     */
    onTypeaheadLoaded: PropTypes.func,
    /**
     * Function that will be called when a suggestion is selected. The
     * function will receive the typeahead event as first argument, and
     * the selected suggestion as second argument. The selected suggestion
     * will be returned in an object, as a value for the `displayKey` key,
     * e.g. `{ suggestion: 'Foo' }`.
     *
     * For more information on typeahead events, see:
     * https://github.com/twitter/typeahead.js/blob/master/doc/jquery_typeahead.md#custom-events
     *
     */
    onSuggestionSelected: PropTypes.func,
  };

  static defaultProps = {
    displayKey: 'suggestion',
  };

  componentDidMount() {
    this._updateTypeahead(this.props);
  }

  componentWillReceiveProps(newProps) {
    this._destroyTypeahead();
    this._updateTypeahead(newProps);
  }

  componentWillUnmount() {
    this._destroyTypeahead();
  }

  getValue = () => {
    return $(this.fieldInput).typeahead('val');
  };

  clear = () => {
    $(this.fieldInput).typeahead('val', '');
  };

  _destroyTypeahead = () => {
    $(this.fieldInput).typeahead('destroy');
    $(this.fieldFormGroup).off('typeahead:select typeahead:autocomplete');
  };

  _updateTypeahead = (props) => {
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
          return `<div>${value[props.displayKey]}</div>`;
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
  };

  render() {
    return (<Input id={this.props.id}
                   type="text"
                   ref="fieldInput"
                   wrapperClassName="typeahead-wrapper"
                   label={this.props.label}
                   onKeyPress={this.props.onKeyPress} />);
  }
}

export default TypeAheadInput;
