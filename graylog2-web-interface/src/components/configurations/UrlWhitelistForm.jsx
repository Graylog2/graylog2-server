// @flow strict
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import uuid from 'uuid/v4';
import URI from 'urijs';
import Input from 'components/bootstrap/Input';
import { Select, Icon } from 'components/common';
import { Button, Table } from 'components/graylog';
import ObjectUtils from 'util/ObjectUtils';
import { get } from 'lodash';
import FormUtils from 'util/FormsUtils';
import type { Url, Config } from 'stores/configurations/ConfigurationsStore';
import StoreProvider from 'injection/StoreProvider';

const ToolsStore = StoreProvider.getStore('Tools');


type Props = {
  urls: Array<Url>,
  disabled: boolean,
  update: (config: Config, valid: boolean) => void
};
const UrlWhitelistForm = ({ urls, update, disabled }: Props) => {
  const literal = 'literal';
  const regex = 'regex';
  const options = [{ value: literal, label: 'Literal' }, { value: regex, label: 'Regex' }];
  // eslint-disable-next-line prefer-const
  let inputs = {};
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

  const validURL = (str: string) => {
    let isValid = true;
    try {
      // eslint-disable-next-line no-unused-vars
      const test = new URL(str);
    } catch (e) {
      isValid = false;
    }
    return isValid;
  };

  const validate = (name: string, idx: number, type: string, value: string) => {
    const validationUpdate = ObjectUtils.clone(validationState);
    switch (name) {
      case 'title':
        validationUpdate.errors[idx].name = value.trim().length <= 0 ? { valid: false } : { valid: true };
        break;
      case 'value':
        if (type === literal) {
          validationUpdate.errors[idx].value = validURL(value) ? { valid: true } : { valid: false };
        } else {
          const promise = ToolsStore.testRegexValidity(value);
          promise.then((result) => {
            validationUpdate.errors[idx].value = result.is_valid ? { valid: true } : { valid: true };
          });
        }
        break;
      default:
        break;
    }
    setValidationState(validationUpdate);
    console.log(validationState);
  };
  const isFormValid = () => {
    let isValid = true;
    if (validationState.errors.find((el => el.title.valid === false || el.value.valid === false))) {
      isValid = false;
    }
    return isValid;
  };
  const onInputChange = (event: SyntheticInputEvent<EventTarget>, idx: number, type: string) => {
    console.log(inputs[event.target.name + idx]);
    validate(event.target.name, idx, type, FormUtils.getValueFromInput(event.target));
    // const stateUpdate = ObjectUtils.clone(state);
    // stateUpdate.entries[idx][event.target.name] = FormUtils.getValueFromInput(event.target);
    // setState(stateUpdate);
  };

  const onUpdateType = (idx: number, value: string) => {
    const stateUpdate = ObjectUtils.clone(state);
    stateUpdate.entries[idx] = { ...state.entries[idx], value };
    setState(stateUpdate);
  };

  useEffect(() => {
    const valid = true;
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
                         ref={(elem) => { inputs[`title${idx}`] = elem; }}
                         name="title"
                         bsStyle={validationState.errors[idx] && validationState.errors[idx].title && !validationState.errors[idx].title.valid ? 'error' : null}
                         onChange={event => onInputChange(event, idx, url.type)}
                         defaultValue={url.title}
                         required />
                </td>
                <td>
                  <Input type="text"
                         ref={(elem) => { inputs[`value${idx}`] = elem; }}
                         name="value"
                         bsStyle={validationState.errors[idx] && validationState.errors[idx].value && !validationState.errors[idx].value.valid ? 'error' : null}
                         onChange={event => onInputChange(event, idx, url.type)}
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
