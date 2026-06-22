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
import styled from 'styled-components';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { StreamsAndCategoriesSelection } from 'views/components/common/StreamsAndCategoriesFilter';
import StreamsAndCategoriesFilter from 'views/components/common/StreamsAndCategoriesFilter';

const Container = styled.div`
  flex: 1;
  grid-area: streams;
`;

type Props = {
  disabled?: boolean;
  value?: StreamsAndCategoriesSelection;
  streams: Array<{ key: string; value: string }>;
  streamCategories: Array<{ key: string; value: string }>;
  onChange: (value: StreamsAndCategoriesSelection) => void;
  multi?: boolean;
  clearable?: boolean;
};

const StreamsFilter = ({
  disabled = false,
  value = { streams: [], categories: [] },
  streams,
  streamCategories,
  onChange,
  multi = true,
  clearable = true,
}: Props) => {
  const sendTelemetry = useSendTelemetry();
  const placeholder = 'Select streams the search should include. Searches in all streams if empty.';

  const handleChange = (selected: StreamsAndCategoriesSelection) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_STREAM_INPUT_CHANGED, {
      app_pathname: 'search',
      app_section: 'search-bar',
      app_action_value: 'search-filter',
      event_details: {
        streamsCount: selected?.streams?.length,
      },
    });

    onChange(selected);
  };

  return (
    <Container data-testid="streams-filter" title={placeholder}>
      <StreamsAndCategoriesFilter
        placeholder={placeholder}
        disabled={disabled}
        clearable={clearable}
        aria-label={placeholder}
        inputId="streams-filter"
        onChange={handleChange}
        streams={streams.map((v) => ({ id: v.value, title: v.key, categories: [] }))}
        streamCategories={streamCategories.map((v) => v.value)}
        multi={multi}
        value={value}
        required={false}
      />
    </Container>
  );
};

export default StreamsFilter;
