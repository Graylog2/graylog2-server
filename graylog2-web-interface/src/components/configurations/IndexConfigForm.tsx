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
import React, { useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import { cloneDeep, debounce, map } from 'lodash';
import styled from 'styled-components';

import Input from 'components/bootstrap/Input';
// Explicit import to fix eslint import/no-cycle
import Select from 'components/common/Select';
import Icon from 'components/common/Icon';
import { Button, Table } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import type { IndexConfig } from 'stores/configurations/IndexConfig';
import ToolsStore from 'stores/tools/ToolsStore';
import { isValidURL } from 'util/URLUtils';
import generateId from 'logic/generateId';

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

type Props = {
  config: Array,
  onUpdate: (config: IndexConfig, valid: boolean) => void,
  newEntryId?: string,
};

const UrlWhiteListForm = ({ config, onUpdate, newEntryId }: Props) => {
  const literal = 'literal';
  const regex = 'regex';
  const options = [{ value: literal, label: 'Exact match' }, { value: regex, label: 'Regex' }];
  // eslint-disable-next-line prefer-const
  let inputs = {};
  const [config, setConfig] = useState<IndexConfig>({ entries: config});
  const [validationState, setValidationState] = useState({ errors: [] });
  const isInitialRender = useRef<boolean>(false);

  const _onAdd = (event: Event) => {
    event.preventDefault();
    setConfig({ ...config, entries: [...config.entries, { id: generateId(), title: '', value: '', type: literal }] });
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

  const hasValidationErrors = useCallback(() => {
    let isValid = true;

    if (validationState.errors.length > 0
      && validationState.errors.find(((el) => (el && el.title && el.title.valid) === false
        || (el && el.value && el.value.valid === false)))) {
      isValid = false;
    }

    return isValid;
  }, [validationState]);

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

  const _validate = async (name: string, idx: number, value: string): Promise<void> => {
    const nextEntry = { ...config.entries[idx], [name]: value };
    await debouncedValidateUrlEntry(idx, nextEntry, _updateValidationError);
  };

  const _onInputChange = (event: React.ChangeEvent<HTMLInputElement>, idx: number) => {
    _validate(event.target.name, idx, getValueFromInput(event.target));
  };

  const _onUpdateType = (idx: number, type: string) => {
    _validate('type', idx, type);
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
                   onChange={(event) => _onInputChange(event, idx)}
                   defaultValue={url.title}
                   required />
            <Input id="index-prefix-field"
                   type="text"

                   label="Index Prefix"
                   help=""
                   value={config.indexPrefix} />
          </td>
          <td>
            <Input type="text"
                   id={`value-input${idx}`}
                   ref={(elem) => { inputs[`value${idx}`] = elem; }}
                   help={validationState.errors[idx] && validationState.errors[idx].value && !validationState.errors[idx].value.valid ? _getErrorMessage(url.type) : null}
                   name="value"
                   bsStyle={validationState.errors[idx] && validationState.errors[idx].value && !validationState.errors[idx].value.valid ? 'error' : null}
                   onChange={(event) => _onInputChange(event, idx)}
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
    const isNewEntryValid = async () => {
      const newEntryIdx = config.entries.findIndex((entry) => entry.id === newEntryId);

      if (newEntryIdx < 0) {
        return false;
      }

      const newEntry = config.entries[newEntryIdx];
      const entryValidation = await validateUrlEntry(newEntryIdx, newEntry, _updateValidationError);

      return map(entryValidation, 'valid').some((valid) => !!valid);
    };

    const propagateUpdate = async (firstRender) => {
      const valid = firstRender && newEntryId ? await isNewEntryValid() : hasValidationErrors();
      onUpdate(config, valid);
    };

    propagateUpdate(!isInitialRender.current);

    if (!isInitialRender.current) {
      isInitialRender.current = true;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
  newEntryId: PropTypes.string,
};

UrlWhiteListForm.defaultProps = {
  urls: [],
  disabled: false,
  onUpdate: () => {},
  newEntryId: undefined,
};

export default UrlWhiteListForm;
