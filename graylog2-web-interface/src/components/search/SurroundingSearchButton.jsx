// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import naturalSort from 'javascript-natural-sort';

import { DropdownButton, MenuItem } from 'components/graylog';
import DrilldownContext from 'views/components/contexts/DrilldownContext';
import type { SearchesConfig } from './SearchConfig';
import SearchLink from './SearchLink';

const buildTimeRangeOptions = ({ surrounding_timerange_options: surroundingTimerangeOptions = {} }) => Object.entries(surroundingTimerangeOptions)
  .reduce((prev, [key, value]) => ({ ...prev, [moment.duration(key).asSeconds()]: value }), {});

const buildFilterFields = (messageFields, searchConfig) => {
  const { surrounding_filter_fields: surroundingFilterFields = [] } = searchConfig;

  return surroundingFilterFields.reduce((prev, cur) => ({ ...prev, [cur]: messageFields[cur] }), {});
};

const buildSearchLink = (id, from, to, fields, filterFields, streams) => SearchLink.builder()
  .timerange({ type: 'absolute', from, to })
  .streams(streams)
  .filterFields(filterFields)
  .highlightedMessage(id)
  .build()
  .toURL();

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
};

const SurroundingSearchButton = ({ searchConfig, timestamp, id, messageFields }: Props) => {
  const { streams } = useContext(DrilldownContext);
  const timeRangeOptions = buildTimeRangeOptions(searchConfig);
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

export default SurroundingSearchButton;
