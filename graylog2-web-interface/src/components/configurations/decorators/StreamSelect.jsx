// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import SelectContainer from './SelectContainer';
import Select from '../../common/Select';
import { defaultCompare } from '../../../views/logic/DefaultCompare';

export const DEFAULT_STREAM_ID = '000000000000000000000001';
export const DEFAULT_SEARCH_ID = 'DEFAULT_SEARCH';

const StreamSelect = ({ onChange, value, streams }) => {
  const options = [{ label: 'Default Search', value: DEFAULT_SEARCH_ID }, ...streams
    .sort(({ title: key1 }, { title: key2 }) => defaultCompare(key1, key2))
    .map(({ title, id }) => ({ label: title, value: id }))];
  return (
    <SelectContainer>
      <Select inputId="streams-filter"
              onChange={onChange}
              options={streams}
              clearable={false}
              style={{ width: '100%' }}
              placeholder="There are no decorators configured for any stream."
              value={value} />
    </SelectContainer>
  );
};

StreamSelect.propTypes = {};

export default StreamSelect;
