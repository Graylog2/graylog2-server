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
import styled from 'styled-components';

import Select from 'components/common/Select';
import { defaultCompare } from 'logic/DefaultCompare';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const Container = styled.div`
  flex: 1;
  grid-area: streams;
`;

type Props = {
  disabled: boolean,
  value: Array<string>,
  streams: Array<{ key: string, value: string }>,
  onChange: (newStreamIds: Array<string>) => void,
};

const StreamsFilter = ({ disabled, value, streams, onChange }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const selectedStreams = value.join(',');
  const placeholder = 'Select streams the search should include. Searches in all streams if empty.';
  const options = streams.sort(({ key: key1 }, { key: key2 }) => defaultCompare(key1, key2));

  const handleChange = (selected: string) => {
    sendTelemetry('input_value_change', {
      app_pathname: 'search',
      app_section: 'search-bar',
      app_action_value: 'search-filter',
    });

    onChange(selected === '' ? [] : selected.split(','));
  };

  return (
    <Container data-testid="streams-filter" title={placeholder}>
      <Select placeholder={placeholder}
              disabled={disabled}
              inputProps={{ 'aria-label': placeholder }}
              displayKey="key"
              inputId="streams-filter"
              onChange={handleChange}
              options={options}
              multi
              value={selectedStreams} />
    </Container>
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
