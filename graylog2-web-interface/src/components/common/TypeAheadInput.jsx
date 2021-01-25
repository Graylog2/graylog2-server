/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import escape from 'lodash/escape';
import $ from 'jquery';
import 'typeahead.js';
import styled from 'styled-components';

import UniversalSearch from 'logic/search/UniversalSearch';
import { Input } from 'components/bootstrap';

const StyledInput = styled(Input)`
  input&.tt-hint {
    background-color: transparent !important;
  }
`;

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
    /** String that allows overriding the input form group */
    formGroupClassName: PropTypes.string,
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
    formGroupClassName: undefined,
    onKeyPress: () => {},
    onTypeaheadLoaded: () => {},
    onSuggestionSelected: () => {},
    suggestions: [],
    suggestionText: undefined,
  }

  componentDidMount() {
    const { suggestions, displayKey, suggestionText, onTypeaheadLoaded, onSuggestionSelected } = this.props;

    this._updateTypeahead({ suggestions, displayKey, suggestionText, onTypeaheadLoaded, onSuggestionSelected });
  }

  UNSAFE_componentWillReceiveProps(newProps) {
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

  _updateTypeahead = ({ suggestions, displayKey, suggestionText, onTypeaheadLoaded, onSuggestionSelected }) => {
    this.fieldInput = this.fieldInputElem.getInputDOMNode();
    // eslint-disable-next-line react/no-find-dom-node
    this.fieldFormGroup = ReactDOM.findDOMNode(this.fieldInputElem);

    const $fieldInput = $(this.fieldInput);

    $fieldInput.typeahead({
      hint: true,
      highlight: true,
      minLength: 1,
    },
    {
      name: 'dataset-name',
      displayKey: displayKey,
      source: UniversalSearch.substringMatcher(suggestions, displayKey, 6),
      templates: {
        suggestion: (value) => {
          // Escape all text here that may be user-generated, since this is not automatically escaped by React.
          if (suggestionText) {
            return `<div><strong>${escape(suggestionText)}</strong> ${escape(value[displayKey])}</div>`;
          }

          return `<div>${escape(value[displayKey])}</div>`;
        },
      },
    });

    if (typeof onTypeaheadLoaded === 'function') {
      onTypeaheadLoaded();
      $fieldInput.typeahead('close');
    }

    $(this.fieldFormGroup).on('typeahead:select typeahead:autocomplete', (event, suggestion) => {
      if (onSuggestionSelected) {
        onSuggestionSelected(event, suggestion);
      }
    });
  };

  render() {
    const { id, label, onKeyPress, formGroupClassName } = this.props;

    return (
      <StyledInput id={id}
                   type="text"
                   ref={(fieldInput) => { this.fieldInputElem = fieldInput; }}
                   wrapperClassName="typeahead-wrapper"
                   formGroupClassName={formGroupClassName}
                   label={label}
                   onKeyPress={onKeyPress} />
    );
  }
}

export default TypeAheadInput;
