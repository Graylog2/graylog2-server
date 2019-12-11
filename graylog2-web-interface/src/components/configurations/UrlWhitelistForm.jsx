// @flow strict
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import uuid from 'uuid/v4';
import Input from 'components/bootstrap/Input';
import { Select, Icon } from 'components/common';
import { Button, Table } from 'components/graylog';
import ObjectUtils from 'util/ObjectUtils';
import { get } from 'lodash';
import FormUtils from 'util/FormsUtils';
import type { Url, Config } from 'stores/configurations/ConfigurationsStore';


type Props = {
  urls: Array<Url>,
  disabled: boolean,
  update: (config: Config, valid: boolean) => void
};
const UrlWhitelistForm = ({ urls, update, disabled }: Props) => {
  const literal = 'literal';
  const regex = 'regex';
  const options = [{ value: literal, label: 'Literal' }, { value: regex, label: 'Regex' }];
  const inputs = {};
  const [state, setState] = useState<Config>({ entries: urls, disabled });
  const [validationState, setValidationState] = useState({ errors: [] });
  const onAdd = (event: Event) => {
    event.preventDefault();
    setState({ ...state, entries: [...state.entries, { id: uuid(), title: '', value: '', type: literal }] });
  };
  const onRemove = (event: MouseEvent, idx: number) => {
    event.preventDefault();
    // eslint-disable-next-line prefer-const
    let stateUpdate = ObjectUtils.clone(state);
    stateUpdate.entries.splice(idx, 1);
    setState(stateUpdate);
  };
  const validURL = (str: string): boolean => {
    const expression = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;
    const regexp = new RegExp(expression);
    return regexp.test(str);
  };
  const validRegex = (str: string) => {
    let isValid = true;
    try {
      const expression = new RegExp(str);
      console.log(expression);
    } catch (e) {
      isValid = false;
    }
    return isValid;
  };

  const isValid = (input: string, value: string, type: string) => {
    switch (input) {
      case 'title':
        return value.trim().length > 0;
      case 'value':
        if (type === literal) {
          return validURL(value);
        }
        return validURL(value) && validRegex(value);
      default:
        break;
    }
  };
  const validate = (input: string, value: string, type: string) => {
    if (isValid(input, value, type)) {
      return { valid: true };
    }
    return { valid: false };
  };

  const validateForm = () => {
    let isFormValid = true;
    const validationUpdate = ObjectUtils.clone(validationState);
    state.entries.forEach((url, idx) => {
      validationUpdate.errors[idx] = {
        title: validate('title', url.title, url.type),
        value: validate('value', url.value, url.type),
      };
    });
    if (validationUpdate.errors.find((el => el.title.valid === false || el.value.valid === false))) {
      isFormValid = false;
    }
    setValidationState(validationUpdate);
    return isFormValid;
  };
  const onInputChange = (event: SyntheticInputEvent<EventTarget>, idx: number) => {
    const stateUpdate = ObjectUtils.clone(state);
    stateUpdate.entries[idx][event.target.name] = FormUtils.getValueFromInput(event.target);
    setState(stateUpdate);
  };

  const onUpdateType = (idx, value: string) => {
    const stateUpdate = ObjectUtils.clone(state);
    stateUpdate.entries[idx] = { ...state.entries[idx], value };
    setState(stateUpdate);
  };


  useEffect(() => {
    const valid = validateForm();
    update(state, valid);
  }, [state]);

  return (
    <>
      <Input type="checkbox"
             id="whitelist-disabled"
             label="Disabled"
             checked={state.disabled}
             onChange={() => setState({ ...state, disabled: !state.disabled })}
             help="Disable this white list." />
      <Table striped bordered className="top-margin">
        <thead>
          <tr>
            <th>#</th>
            <th>Title</th>
            <th>Url</th>
            <th>Type</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {state.entries.map((url, idx) => {
            return (
            // eslint-disable-next-line react/no-array-index-key
              <tr key={idx + 1}>
                <td>{idx + 1}</td>
                <td>
                  <Input type="text"
                         ref={(elem) => { inputs[`titleRef${idx}`] = elem; }}
                         name="title"
                         bsStyle={validationState.errors[idx] && validationState.errors[idx].title && !validationState.errors[idx].title.valid ? 'error' : null}
                         help={get(validationState.errors[idx], 'title.valid', 'dsdsd')}
                         onChange={event => onInputChange(event, idx)}
                         defaultValue={url.title}
                         required />
                </td>
                <td>
                  <Input type="text"
                         ref={(elem) => { inputs[`urlRref${idx}`] = elem; }}
                         name="value"
                         bsStyle={validationState.errors[idx] && validationState.errors[idx].value && !validationState.errors[idx].value.valid ? 'error' : null}
                         onChange={event => onInputChange(event, idx)}
                         defaultValue={url.value}
                         required />
                </td>
                <td>
                  <Input id={`url-input-type-${idx}`}
                         required
                         autoFocus>
                    <Select placeholder="Select Cache Type"
                            clearable={false}
                            options={options}
                            matchProp="label"
                            onChange={option => onUpdateType(idx, option)}
                            value={url.type} />
                  </Input>
                </td>
                <td>
                  <span className="">
                    <Icon name="fa-trash" style={{ cursor: 'pointer' }} onClick={event => onRemove(event, idx)} />
                  </span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </Table>
      <Button bsSize="xs" onClick={event => onAdd(event)}>Add Url</Button>
    </>
  );
};

UrlWhitelistForm.propTypes = {
  urls: PropTypes.array,
  update: PropTypes.func,
};

UrlWhitelistForm.defaultProps = {
  urls: [],
  update: () => {},
};

export default UrlWhitelistForm;
