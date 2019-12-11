// @flow strict
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import uuid from 'uuid/v4';
import Input from 'components/bootstrap/Input';
import { Select, Icon } from 'components/common';
import { Button, Table } from 'components/graylog';
import ObjectUtils from 'util/ObjectUtils';
import type { Url, Config } from 'stores/configurations/ConfigurationsStore';

type Props = {
  urls: Array<Url>,
  disabled: boolean,
  update: (config: Config) => void
};
const UrlWhitelistForm = ({ urls, update, disabled }: Props) => {
  const options = [{ value: 'literal', label: 'Literal' }, { value: 'regex', label: 'Regex' }];
  const inputs = {};
  const [state, setState] = useState<Config>({ entries: urls, disabled });
  const onAdd = (event: Event) => {
    event.preventDefault();
    setState({ ...state, entries: [...state.entries, { id: uuid(), title: '', value: '', type: 'literal' }] });
  };
  const onRemove = (event: MouseEvent, idx: number) => {
    event.preventDefault();
    // eslint-disable-next-line prefer-const
    let stateUpdate = ObjectUtils.clone(state);
    stateUpdate.entries.splice(idx, 1);
    setState(stateUpdate);
  };

  const onInputChange = (event: SyntheticInputEvent<EventTarget>, idx: number) => {
    const stateUpdate = ObjectUtils.clone(state);
    stateUpdate.entries[idx][event.target.name] = event.target.value;
    setState(stateUpdate);
  };

  const onUpdateUrl = (idx, type: string, value: string) => {
    const stateUpdate = ObjectUtils.clone(state);
    stateUpdate.entries[idx] = { ...state.entries[idx], value, type };
    setState(stateUpdate);
  };

  useEffect(() => {
    update(state);
  }, [state]);

  return (
    <>
      <Input type="checkbox"
             id="whitelist-disabled"
             label="Disabled"
             checked={state.disabled}
             onChange={() => setState({ ...state, disabled: !state.disabled })}
             help="Disable this white list." />
      <Table striped bordered condense className="top-margin">
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
                         onChange={event => onInputChange(event, idx)}
                         defaultValue={url.title}
                         required />
                </td>
                <td>
                  <Input type="text"
                         ref={(elem) => { inputs[`urlRref${idx}`] = elem; }}
                         name="value"
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
                            onChange={option => onUpdateUrl(idx, option, url.value)}
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
