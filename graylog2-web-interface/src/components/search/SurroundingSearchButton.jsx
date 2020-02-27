// @flow strict
import PropTypes from 'prop-types';
import * as React from 'react';
import Qs from 'qs';
import moment from 'moment';

import Routes from 'routing/Routes';
import { DropdownButton, MenuItem } from 'components/graylog';
import naturalSort from 'javascript-natural-sort';
import { addToQuery, escape } from 'views/logic/queries/QueryHelper';

const buildTimeRangeOptions = ({ surrounding_timerange_options: surroundingTimerangeOptions = {} }) => Object.entries(surroundingTimerangeOptions)
  .reduce((prev, [key, value]) => ({ ...prev, [moment.duration(key).asSeconds()]: value }), {});

const buildFilterFields = (messageFields, searchConfig) => {
  const { surrounding_filter_fields: surroundingFilterFields = [] } = searchConfig;

  return surroundingFilterFields.reduce((prev, cur) => ({ ...prev, [cur]: messageFields[cur] }), {});
};

const buildSearchLink = (id, fromTime, toTime, fields, filter) => {
  const query = Object.keys(filter)
    .filter(key => (filter[key] !== null && filter[key] !== undefined))
    .map(key => `${key}:"${escape(filter[key])}"`)
    .reduce((prev, cur) => addToQuery(prev, cur), '');

  const params = {
    rangetype: 'absolute',
    from: fromTime,
    to: toTime,
    q: query,
    highlightMessage: id,
    fields,
  };

  return `${Routes.SEARCH}?${Qs.stringify(params)}`;
};

const searchLink = (range, timestamp, id, messageFields, searchConfig) => {
  const fromTime = moment(timestamp).subtract(Number(range), 'seconds').toISOString();
  const toTime = moment(timestamp).add(Number(range), 'seconds').toISOString();
  const filterFields = buildFilterFields(messageFields, searchConfig);

  return buildSearchLink(id, fromTime, toTime, [], filterFields);
};

type Props = {
  searchConfig: {
    surrounding_timerange_options: {},
    surrounding_filter_fields: Array<string>,
  },
  timestamp: string,
  id: string,
  messageFields: { [string]: mixed },
};

const SurroundingSearchButton = ({ searchConfig, timestamp, id, messageFields }: Props) => {
  const timeRangeOptions = buildTimeRangeOptions(searchConfig);
  const menuItems = Object.keys(timeRangeOptions)
    .sort((a, b) => naturalSort(a, b))
    .map((range) => (
      <MenuItem key={range} href={searchLink(range, timestamp, id, messageFields, searchConfig)}>
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
  timestamp: PropTypes.number.isRequired,
  searchConfig: PropTypes.object.isRequired,
  messageFields: PropTypes.object.isRequired,
};

export default SurroundingSearchButton;
