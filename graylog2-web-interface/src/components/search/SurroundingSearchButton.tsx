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
import PropTypes from 'prop-types';
import moment from 'moment';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { DropdownButton, MenuItem } from 'components/bootstrap';
import DrilldownContext from 'views/components/contexts/DrilldownContext';

import type { SearchesConfig } from './SearchConfig';
import SearchLink from './SearchLink';

const buildFilterFields = (messageFields, searchConfig) => {
  const { surrounding_filter_fields: surroundingFilterFields = [] } = searchConfig;

  return surroundingFilterFields.reduce((prev, cur) => ({ ...prev, [cur]: messageFields[cur] }), {});
};

const buildSearchLink = (id, from, to, filterFields, streams) => SearchLink.builder()
  .timerange({ type: 'absolute', from, to })
  .streams(streams)
  .filterFields(filterFields)
  .highlightedMessage(id)
  .build()
  .toURL();

const searchLink = (range, timestamp, id, messageFields, searchConfig, streams) => {
  const rangeSeconds = Number(moment.duration(range).asSeconds());
  const fromTime = moment(timestamp).subtract(rangeSeconds, 'seconds').toISOString();
  const toTime = moment(timestamp).add(rangeSeconds, 'seconds').toISOString();
  const filterFields = buildFilterFields(messageFields, searchConfig);

  return buildSearchLink(id, fromTime, toTime, filterFields, streams);
};

type Props = {
  searchConfig: SearchesConfig,
  timestamp: string,
  id: string,
  messageFields: { [key: string]: unknown },
};

const SurroundingSearchButton = ({ searchConfig, timestamp, id, messageFields }: Props) => {
  const { streams } = useContext(DrilldownContext);
  const { surrounding_timerange_options: surroundingTimerangeOptions } = searchConfig;
  const menuItems = Object.keys(surroundingTimerangeOptions)
    .map((range) => (
      <MenuItem key={range}
                href={searchLink(range, timestamp, id, messageFields, searchConfig, streams)}
                target="_blank"
                rel="noopener noreferrer">
        {surroundingTimerangeOptions[range]}
      </MenuItem>
    ));

  return (
    <DropdownButton title="Show surrounding messages" bsSize="small" id="surrounding-search-dropdown">
      {menuItems}
    </DropdownButton>
  );
};

SurroundingSearchButton.propTypes = {
  id: PropTypes.string.isRequired,
  timestamp: PropTypes.string.isRequired,
  searchConfig: PropTypes.object.isRequired,
  messageFields: PropTypes.object.isRequired,
};

export default SurroundingSearchButton;
