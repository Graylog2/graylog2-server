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
import PropTypes from 'prop-types';

import Select from 'components/common/Select';
import { defaultCompare } from 'logic/DefaultCompare';

type Props = {
  disabled: boolean,
  value: Array<string>,
  streams: Array<{ key: string, value: string }>,
  onChange: (newStreamIds: Array<string>) => void,
};

const StreamsFilter = ({ disabled, value, streams, onChange }: Props) => {
  const selectedStreams = value.join(',');
  const placeholder = 'Select streams the search should include. Searches in all streams if empty.';
  const options = streams.sort(({ key: key1 }, { key: key2 }) => defaultCompare(key1, key2));

  return (
    <div style={{ position: 'relative' }} data-testid="streams-filter" title={placeholder}>
      <Select placeholder={placeholder}
              disabled={disabled}
              inputProps={{ 'aria-label': placeholder }}
              displayKey="key"
              inputId="streams-filter"
              onChange={(selected: string) => onChange(selected === '' ? [] : selected.split(','))}
              options={options}
              multi
              value={selectedStreams} />
    </div>
  );
};

StreamsFilter.propTypes = {
  disabled: PropTypes.bool,
  value: PropTypes.arrayOf(PropTypes.string),
  streams: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      value: PropTypes.string.isRequired,
    }),
  ).isRequired,
  onChange: PropTypes.func.isRequired,
};

StreamsFilter.defaultProps = {
  disabled: false,
  value: [],
};

export default StreamsFilter;
