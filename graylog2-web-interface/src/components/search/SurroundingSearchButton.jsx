// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import Qs from 'qs';
import moment from 'moment';
import naturalSort from 'javascript-natural-sort';

import Routes from 'routing/Routes';
import connect from 'stores/connect';
import { DropdownButton, MenuItem } from 'components/graylog';
import { addToQuery, escape } from 'views/logic/queries/QueryHelper';
import { QueryFiltersStore } from 'views/stores/QueryFiltersStore';
import type { FilterType } from 'views/logic/queries/Query';
import { filtersToStreamSet } from 'views/logic/queries/Query';
import type { SearchesConfig } from './SearchConfig';

const buildTimeRangeOptions = ({ surrounding_timerange_options: surroundingTimerangeOptions = {} }) => Object.entries(surroundingTimerangeOptions)
  .reduce((prev, [key, value]) => ({ ...prev, [moment.duration(key).asSeconds()]: value }), {});

const buildFilterFields = (messageFields, searchConfig) => {
  const { surrounding_filter_fields: surroundingFilterFields = [] } = searchConfig;

  return surroundingFilterFields.reduce((prev, cur) => ({ ...prev, [cur]: messageFields[cur] }), {});
};

const buildSearchLink = (id, fromTime, toTime, fields, filterFields, streams) => {
  const query = Object.keys(filterFields)
    .filter(key => (filterFields[key] !== null && filterFields[key] !== undefined))
    .map(key => `${key}:"${escape(filterFields[key])}"`)
    .reduce((prev, cur) => addToQuery(prev, cur), '');

  const params = {
    rangetype: 'absolute',
    from: fromTime,
    to: toTime,
    q: query,
    highlightMessage: id,
    fields,
  };
  const paramsWithStreams = streams && streams.length > 0
    ? { ...params, streams: streams.join(',') }
    : params;

  return `${Routes.SEARCH}?${Qs.stringify(paramsWithStreams)}`;
};

const searchLink = (range, timestamp, id, messageFields, searchConfig, streams) => {
  const fromTime = moment(timestamp).subtract(Number(range), 'seconds').toISOString();
  const toTime = moment(timestamp).add(Number(range), 'seconds').toISOString();
  const filterFields = buildFilterFields(messageFields, searchConfig);

  return buildSearchLink(id, fromTime, toTime, [], filterFields, streams);
};

type Props = {
  searchConfig: SearchesConfig,
  timestamp: string,
  id: string,
  messageFields: { [string]: mixed },
  queryFilters: ?FilterType,
};

const SurroundingSearchButton = ({ searchConfig, timestamp, id, messageFields, queryFilters }: Props) => {
  const timeRangeOptions = buildTimeRangeOptions(searchConfig);
  const streams = filtersToStreamSet(queryFilters).toJS();
  const menuItems = Object.keys(timeRangeOptions)
    .sort((a, b) => naturalSort(a, b))
    .map(range => (
      <MenuItem key={range} href={searchLink(range, timestamp, id, messageFields, searchConfig, streams)} target="_blank" rel="noopener noreferrer">
        {timeRangeOptions[range]}
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

export default connect(
  SurroundingSearchButton,
  { queryFilters: QueryFiltersStore },
  ({ queryFilters = Immutable.Map() }) => ({ queryFilters: queryFilters.first() }),
);
