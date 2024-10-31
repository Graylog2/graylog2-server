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
import * as React from 'react';
import { useContext } from 'react';
import moment from 'moment';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { DropdownButton, MenuItem } from 'components/bootstrap';
import DrilldownContext from 'views/components/contexts/DrilldownContext';
import SearchLink from 'views/logic/search/SearchLink';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';

import type { SearchesConfig } from './SearchConfig';

const buildTimeRangeOptions = ({ surrounding_timerange_options: surroundingTimerangeOptions = {} }: Pick<SearchesConfig, 'surrounding_timerange_options'>) => Object.fromEntries(
  Object.entries(surroundingTimerangeOptions).map(([key, value]) => [moment.duration(key).asSeconds(), value]),
);

const buildFilterFields = (messageFields: {
  [x: string]: unknown;
}, searchConfig: Pick<SearchesConfig, 'surrounding_filter_fields'>) => {
  const { surrounding_filter_fields: surroundingFilterFields = [] } = searchConfig;

  return Object.fromEntries(surroundingFilterFields.map((fieldName) => [fieldName, messageFields[fieldName]]));
};

const buildSearchLink = (id: string, from: string, to: string, filterFields: {
  [key: string]: unknown;
}, streams: string[], streamCategories: string[]) => SearchLink.builder()
  .timerange({ type: 'absolute', from, to })
  .streams(streams)
  .streamCategories(streamCategories)
  .filterFields(filterFields)
  .highlightedMessage(id)
  .build()
  .toURL();

const searchLink = (range: string, timestamp: moment.MomentInput, id: string, messageFields: {
  [key: string]: unknown;
}, searchConfig: Pick<SearchesConfig, 'surrounding_filter_fields'>, streams: string[], streamCategories: string[]) => {
  const fromTime = moment(timestamp).subtract(Number(range), 'seconds').toISOString();
  const toTime = moment(timestamp).add(Number(range), 'seconds').toISOString();
  const filterFields = buildFilterFields(messageFields, searchConfig);

  return buildSearchLink(id, fromTime, toTime, filterFields, streams, streamCategories);
};

type Props = {
  searchConfig: Pick<SearchesConfig, 'surrounding_timerange_options' | 'surrounding_filter_fields'>,
  timestamp: string,
  id: string,
  messageFields: { [key: string]: unknown },
};

const SurroundingSearchButton = ({ searchConfig, timestamp, id, messageFields }: Props) => {
  const { streams, streamCategories } = useContext(DrilldownContext);
  const timeRangeOptions = buildTimeRangeOptions(searchConfig);
  const location = useLocation();
  const sendTelemetry = useSendTelemetry();

  const sendEvent = (range: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_MESSAGE_TABLE_SHOW_SURROUNDING_MESSAGE, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'search-message-table',
      app_action_value: 'seach-message-table-show-surrounding',
      event_details: {
        range,
      },
    });
  };

  const menuItems = Object.keys(timeRangeOptions)
    .sort((a, b) => naturalSort(a, b))
    .map((range) => (
      <MenuItem key={range}
                onClick={() => sendEvent(range)}
                href={searchLink(range, timestamp, id, messageFields, searchConfig, streams, streamCategories)}
                target="_blank"
                rel="noopener noreferrer">
        {timeRangeOptions[range]}
      </MenuItem>
    ));

  return (
    <DropdownButton title="Show surrounding messages" bsSize="small" id="surrounding-search-dropdown">
      {menuItems}
    </DropdownButton>
  );
};

export default SurroundingSearchButton;
