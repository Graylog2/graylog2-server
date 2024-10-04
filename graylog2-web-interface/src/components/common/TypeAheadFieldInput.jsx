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
import Immutable from 'immutable';
import $ from 'jquery';
import 'typeahead.js';
import styled, { css } from 'styled-components';

import { Input } from 'components/bootstrap';
import UniversalSearch from 'logic/search/UniversalSearch';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

const Container = styled.div(({ theme }) => css`
  width: 100%;

  .twitter-typeahead {
    width: 100%;
  }

  .typeahead-wrapper {
    display: inline-block;
    vertical-align: middle;
    width: 100%;
  }

  .typeahead,
  .tt-query,
  .tt-hint {
    border: 2px solid #ccc;
    border-radius: 4px;
    outline: none;
  }

  .typeahead {
    background-color: #fff;
  }

  .typeahead:focus {
    border: 2px solid #0097cf;
  }

  .tt-query {
    box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075);
  }

  input[type="text"].tt-hint {
    color: #999
  }

  .tt-menu {
    min-width: 160px;
    //background-color: #fff;
    border: 1px solid rgba(0, 0, 0, 0.2);
    border-radius: 4px;
    //box-shadow: 0 5px 10px rgba(0,0,0,.2);
    width: 100%;
    background-color: ${theme.colors.global.contentBackground};
    box-shadow: 0 3px 3px ${theme.colors.global.navigationBoxShadow};
    color: ${theme.colors.global.textDefault};

    .tt-suggestion:hover,
    .tt-suggestion.tt-cursor {
      color: ${theme.colors.variant.darkest.info};
      background-color: ${theme.colors.variant.lighter.info};
      background-image: none;
    }
  }

  .tt-dataset {
    margin-top: 10px;
  }

  .tt-suggestion {
    font-size: 1rem; /* theme.fonts.size.body */
    line-height: 20px;
    padding: 3px 10px;
    cursor: pointer;
  }

  .tt-suggestion:hover,
  .tt-suggestion.tt-cursor {
    color: #ffffff;
    text-decoration: none;
    background-color: #0081c2;
    background-image: linear-gradient(to bottom, #0088cc, #0077b3);
    background-repeat: repeat-x;
    outline: 0;
    filter: progid:DXImageTransform.Microsoft.gradient(startColorstr='#ff0088cc', endColorstr='#ff0077b3', GradientType=0);
  }

  .tt-suggestion p {
    margin: 0;
  }
`);

/**
 * Component that renders an input offering auto-completion for message fields.
 * Fields are loaded from the Graylog server in the background.
 */
class TypeAheadFieldInput extends React.Component {
  static propTypes = {
    /** ID of the input. */
    id: PropTypes.string.isRequired,
    /**
     * @deprecated React v15 deprecated `valueLink`s. Please use `onChange`
     * instead.
     */
    valueLink: PropTypes.object,
    /** Label of the field input */
    label: PropTypes.string,
    /** Specifies if the input should have the input focus or not. */
    autoFocus: PropTypes.bool,
    /**
     * Function that is called when the input changes. The function receives
     * the typeahead event object for the event that triggered the change. For
     * more information on typeahead events, see:
     * https://github.com/twitter/typeahead.js/blob/master/doc/jquery_typeahead.md#custom-events
     */
    onChange: PropTypes.func,
    onBlur: PropTypes.func,
    /** Display an error for the input * */
    error: PropTypes.string,
  };

  static defaultProps = {
    valueLink: undefined,
    autoFocus: false,
    label: undefined,
    onChange: () => {},
    onBlur: () => {},
    error: undefined,
  };

  componentDidMount() {
    if (this.fieldInput) {
      const { autoFocus, valueLink, onChange } = this.props;
      const fieldInput = $(this.fieldInput.getInputDOMNode());

      fetch('GET', qualifyUrl(ApiRoutes.SystemApiController.fields().url))
        .then(
          (data) => {
            fieldInput.typeahead(
              {
                hint: true,
                highlight: true,
                minLength: 1,
              },
              {
                name: 'fields',
                displayKey: 'value',
                source: UniversalSearch.substringMatcher(data.fields, 'value', 6),
              },
            );

            if (autoFocus) {
              fieldInput.focus();
              fieldInput.typeahead('close');
            }
          },
        );

      // eslint-disable-next-line react/no-find-dom-node
      const fieldFormGroup = ReactDOM.findDOMNode(this.fieldInput);

      $(fieldFormGroup).on('typeahead:change typeahead:selected', (event) => {
        if (onChange) {
          onChange(event);
        }

        if (valueLink) {
          valueLink.requestChange(event.target.value);
        }
      });
    }
  }

  componentWillUnmount() {
    if (this.fieldInput) {
      const fieldInput = $(this.fieldInput.getInputDOMNode());

      fieldInput.typeahead('destroy');

      // eslint-disable-next-line react/no-find-dom-node
      const fieldFormGroup = ReactDOM.findDOMNode(this.fieldInput);

      $(fieldFormGroup).off('typeahead:change typeahead:selected');
    }
  }

  _getFilteredProps = () => {
    let props = Immutable.fromJS(this.props);

    ['valueLink', 'onChange'].forEach((key) => {
      if (props.has(key)) {
        props = props.delete(key);
      }
    });

    return props.toJS();
  };

  render() {
    const { id, label, valueLink, error, onBlur } = this.props;

    return (
      <Container>
        <Input id={id}
               ref={(fieldInput) => { this.fieldInput = fieldInput; }}
               label={label}
               onBlur={onBlur}
               error={error}
               wrapperClassName="typeahead-wrapper"
               defaultValue={valueLink ? valueLink.value : null}
               {...this._getFilteredProps()} />
      </Container>
    );
  }
}

export default TypeAheadFieldInput;
