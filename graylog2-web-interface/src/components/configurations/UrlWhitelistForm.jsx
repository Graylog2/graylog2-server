// @flow strict
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import Input from 'components/bootstrap/Input';
import { Select, Icon } from 'components/common';
import { Button, Table } from 'components/graylog';
import ObjectUtils from 'util/ObjectUtils';
import type { Url } from 'stores/configurations/ConfigurationsStore';

type Props = {
  urls: Array<Url>,
  update: (state: Array<Url>) => void
};
const UrlWhitelistForm = ({ urls, update }: Props) => {
  const options = [{ value: 'literal', label: 'Literal' }, { value: 'regex', label: 'Regex' }];
  const inputs = {};
  const [state, setState] = useState(urls);
  const onAdd = (event) => {
    event.preventDefault();
    setState([...state, { value: '', type: 'literal' }]);
  };
  const onRemove = (event: MouseEvent, idx: number) => {
    event.preventDefault();
    // eslint-disable-next-line prefer-const
    let stateUpdate = ObjectUtils.clone(state);
    stateUpdate.splice(idx, 1);
    setState([...stateUpdate]);
  };

  const onInputChange = (event: KeyboardEvent, idx: number) => {
    state[idx].value = inputs[`ref${idx}`].input.value;
  };

  const onUpdateUrl = (idx, type: string, value: string) => {
    state[idx] = { value, type };
  };

  useEffect(() => {
    update(state);
  }, [state]);

  return (
    <>
      <Table striped bordered condense className="top-margin">
        <thead>
          <tr>
            <th>#</th>
            <th>Url</th>
            <th>Type</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {state.map((url, idx) => {
            return (
            // eslint-disable-next-line react/no-array-index-key
              <tr key={idx + 1}>
                <td>{idx + 1}</td>
                <td>
                  <Input type="text"
                         ref={(elem) => { inputs[`ref${idx}`] = elem; }}
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
