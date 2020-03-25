// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import Select from 'components/common/Select';
import type { Stream } from 'stores/streams/StreamsStore';
import { defaultCompare } from 'views/logic/DefaultCompare';

export const DEFAULT_STREAM_ID = '000000000000000000000001';
export const DEFAULT_SEARCH_ID = 'DEFAULT_SEARCH';

const SelectContainer: StyledComponent<{}, {}, HTMLDivElement> = styled.div`
  margin-bottom: 10px;
`;

type Props = {
  onChange: string => void,
  value: string,
  streams: Array<Stream>,
};

const StreamSelect = ({ onChange, value, streams }: Props) => {
  const options = [{ label: 'Default Search', value: DEFAULT_SEARCH_ID }, ...streams
    .sort(({ title: key1 }, { title: key2 }) => defaultCompare(key1, key2))
    .map(({ title, id }) => ({ label: title, value: id }))];
  return (
    <SelectContainer>
      <Select inputId="streams-filter"
              onChange={onChange}
              options={options}
              clearable={false}
              placeholder="There are no decorators configured for any stream."
              value={value} />
    </SelectContainer>
  );
};

StreamSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
  streams: PropTypes.array.isRequired,
};

export default StreamSelect;
