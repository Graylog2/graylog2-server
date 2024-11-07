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
import React from 'react';
import Immutable from 'immutable';
import $ from 'jquery';
import 'typeahead.js';

import { Input } from 'components/bootstrap';
import UniversalSearch from 'logic/search/UniversalSearch';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import { Container } from './TypeAheadInput';

type TypeAheadFieldInputProps = {
  /** ID of the input. */
  id: string;
  /**
   * @deprecated React v15 deprecated `valueLink`s. Please use `onChange`
   * instead.
   */
  valueLink?: any;
  /** Label of the field input */
  label?: string;
  /** Specifies if the input should have the input focus or not. */
  autoFocus?: boolean;
  /**
   * Function that is called when the input changes. The function receives
   * the typeahead event object for the event that triggered the change. For
   * more information on typeahead events, see:
   * https://github.com/twitter/typeahead.js/blob/master/doc/jquery_typeahead.md#custom-events
   */
  onChange?: (...args: any[]) => void;
  onBlur?: (...args: any[]) => void;
  /** Display an error for the input * */
  error?: string;
  type?: string;
  name?: string;
  defaultValue?: string;
};

/**
 * Component that renders an input offering auto-completion for message fields.
 * Fields are loaded from the Graylog server in the background.
 */
class TypeAheadFieldInput extends React.Component<TypeAheadFieldInputProps> {
  static defaultProps = {
    valueLink: undefined,
    autoFocus: false,
    label: undefined,
    onChange: () => {},
    onBlur: () => {},
    error: undefined,
    type: undefined,
    name: undefined,
    defaultValue: undefined,
  };

  private fieldInput: Input;

  componentDidMount() {
    if (this.fieldInput) {
      const { autoFocus, valueLink, onChange } = this.props;
      const fieldInput = $(this.fieldInput.getInputDOMNode());

      fetch('GET', qualifyUrl(ApiRoutes.SystemApiController.fields().url))
        .then(
          (data) => {
            // @ts-ignore
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
              // @ts-ignore
              fieldInput.typeahead('close');
            }
          },
        );

      const fieldFormGroup = this.fieldInput.getInputDOMNode();

      $(fieldFormGroup).on('typeahead:change typeahead:selected', (event) => {
        if (onChange) {
          onChange(event);
        }

        if (valueLink) {
          valueLink.requestChange((event.target as HTMLInputElement).value);
        }
      });
    }
  }

  componentWillUnmount() {
    if (this.fieldInput) {
      const fieldInput = $(this.fieldInput.getInputDOMNode());

      // @ts-ignore
      fieldInput.typeahead('destroy');

      const fieldFormGroup = this.fieldInput.getInputDOMNode();

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
