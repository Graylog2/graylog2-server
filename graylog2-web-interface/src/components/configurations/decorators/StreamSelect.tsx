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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import type { StyledComponent } from 'styled-components';

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
