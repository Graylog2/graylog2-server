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
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import uuid from 'uuid/v4';
import { cloneDeep, debounce } from 'lodash';
import styled from 'styled-components';

import Input from 'components/bootstrap/Input';
// Explicit import to fix eslint import/no-cycle
import Select from 'components/common/Select';
import Icon from 'components/common/Icon';
import { Button, Table } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import type { Url, WhiteListConfig } from 'stores/configurations/ConfigurationsStore';
import ToolsStore from 'stores/tools/ToolsStore';
import { isValidURL } from 'util/URLUtils';

type Props = {
  urls: Array<Url>,
  disabled: boolean,
  onUpdate: (config: WhiteListConfig, valid: boolean) => void,
};

type ValidationResult = {
  title: { valid: boolean },
  value: { valid: boolean },
};

const StyledTable = styled(Table)`
  margin-top: 10px;
`;

const validateUrlEntry = async (idx: number, entry: Url, callback?: (...any) => void): Promise<ValidationResult> => {
  const validationResult = {
    title: { valid: false },
    value: { valid: false },
  };

  validationResult.title = entry.title.trim().length <= 0 ? { valid: false } : { valid: true };

  let valueValidation = { valid: false };

  if (entry.type === 'literal') {
    valueValidation = isValidURL(entry.value) ? { valid: true } : { valid: false };
  } else if (entry.type === 'regex' && entry.value.trim().length > 0) {
    valueValidation = (await ToolsStore.testRegexValidity(entry.value)).is_valid ? { valid: true } : { valid: false };
  }

  validationResult.value = valueValidation;

  if (typeof callback === 'function') {
    callback(idx, entry, validationResult);
  }

  return validationResult;
};

const debouncedValidateUrlEntry = debounce(validateUrlEntry, 200);

const UrlWhiteListForm = ({ urls, onUpdate, disabled }: Props) => {
  const literal = 'literal';
  const regex = 'regex';
  const options = [{ value: literal, label: 'Exact match' }, { value: regex, label: 'Regex' }];
  // eslint-disable-next-line prefer-const
  let inputs = {};
  const [config, setConfig] = useState<WhiteListConfig>({ entries: urls, disabled });
  const [validationState, setValidationState] = useState({ errors: [] });

  const _onAdd = (event: Event) => {
    event.preventDefault();
    setConfig({ ...config, entries: [...config.entries, { id: uuid(), title: '', value: '', type: literal }] });
  };

  const _onRemove = (event: MouseEvent, idx: number) => {
    event.preventDefault();
    // eslint-disable-next-line prefer-const
    let stateUpdate = cloneDeep(config);
    const validationUpdate = cloneDeep(validationState);

    validationUpdate.errors[idx] = null;
    setValidationState(validationUpdate);
    stateUpdate.entries.splice(idx, 1);
    setConfig(stateUpdate);
  };

  const _isFormValid = (): boolean => {
    let isValid = true;

    if (validationState.errors.length > 0
      && validationState.errors.find(((el) => (el && el.title && el.title.valid) === false
      || (el && el.value && el.value.valid === false)))) {
      isValid = false;
    }

    return isValid;
  };

  const _updateState = (idx: number, nextEntry: Url) => {
    const stateUpdate = cloneDeep(config);
    stateUpdate.entries[idx] = nextEntry;
    setConfig(stateUpdate);
  };

  const _updateValidationError = (idx: number, nextEntry: Url, entryValidation: ValidationResult) => {
    setValidationState((prevValidationState) => {
      const nextValidationState = cloneDeep(prevValidationState);
      nextValidationState.errors[idx] = entryValidation;

      return nextValidationState;
    });

    _updateState(idx, nextEntry);
  };

  const _validate = async (name: string, idx: number, type: string, value: string): Promise<void> => {
    const nextEntry = { ...config.entries[idx], [name]: value };
    await debouncedValidateUrlEntry(idx, nextEntry, _updateValidationError);
  };

  const _onInputChange = (event: React.ChangeEvent<HTMLInputElement>, idx: number, type: string) => {
    _validate(event.target.name, idx, type, getValueFromInput(event.target));
  };

  const _onUpdateType = (idx: number, type: string) => {
    const stateUpdate = cloneDeep(config);

    _validate('value', idx, type, stateUpdate.entries[idx].value);
  };

  const _getErrorMessage = (type: string) => {
    return type === regex ? 'Not a valid Java regular expression' : 'Not a valid URL';
  };

  const _getSummary = () => {
    return (config.entries.map((url, idx) => {
      return (
        <tr key={url.id}>
          <td style={{ verticalAlign: 'middle', textAlign: 'center' }}>{idx + 1}</td>
          <td>
            <Input type="text"
                   id={`title-input${idx}`}
                   ref={(elem) => { inputs[`title${idx}`] = elem; }}
                   help={validationState.errors[idx] && validationState.errors[idx].title && !validationState.errors[idx].title.valid ? 'Required field' : null}
                   name="title"
                   bsStyle={validationState.errors[idx] && validationState.errors[idx].title && !validationState.errors[idx].title.valid ? 'error' : null}
                   onChange={(event) => _onInputChange(event, idx, url.type)}
                   defaultValue={url.title}
                   required />
          </td>
          <td>
            <Input type="text"
                   id={`value-input${idx}`}
                   ref={(elem) => { inputs[`value${idx}`] = elem; }}
                   help={validationState.errors[idx] && validationState.errors[idx].value && !validationState.errors[idx].value.valid ? _getErrorMessage(url.type) : null}
                   name="value"
                   bsStyle={validationState.errors[idx] && validationState.errors[idx].value && !validationState.errors[idx].value.valid ? 'error' : null}
                   onChange={(event) => _onInputChange(event, idx, url.type)}
                   defaultValue={url.value}
                   required />
          </td>
          <td>
            <Input id={`url-input-type-${idx}`}
                   required
                   autoFocus>
              <Select clearable={false}
                      options={options}
                      matchProp="label"
                      placeholder="Select url type"
                      onChange={(option: string) => _onUpdateType(idx, option)}
                      value={url.type} />
            </Input>
          </td>
          <td>
            <Button onClick={(event) => _onRemove(event, idx)}>
              <Icon name="trash-alt" />
              <span className="sr-only">Delete entry</span>
            </Button>
          </td>
        </tr>
      );
    }));
  };

  useEffect(() => {
    const valid = _isFormValid();

    onUpdate(config, valid);
  }, [config]);

  return (
    <>
      <Input type="checkbox"
             id="whitelist-disabled"
             label="Disable Whitelist"
             checked={config.disabled}
             onChange={() => setConfig({ ...config, disabled: !config.disabled })}
             help="Disable the whitelist functionality. Warning: Disabling this option will allow users to enter any URL in Graylog entities, which may pose a security risk." />
      <Button bsSize="sm" onClick={(event) => _onAdd(event)}>Add Url</Button>
      <StyledTable striped bordered>
        <thead>
          <tr>
            <th>#</th>
            <th>Title</th>
            <th>URL</th>
            <th>Type</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {_getSummary()}
        </tbody>
      </StyledTable>
      <Button bsSize="sm" onClick={(event) => _onAdd(event)}>Add Url</Button>
    </>
  );
};

UrlWhiteListForm.propTypes = {
  urls: PropTypes.array,
  disabled: PropTypes.bool,
  onUpdate: PropTypes.func,
};

UrlWhiteListForm.defaultProps = {
  urls: [],
  disabled: false,
  onUpdate: () => {},
};

export default UrlWhiteListForm;
