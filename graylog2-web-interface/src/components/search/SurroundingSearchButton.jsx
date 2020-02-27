import PropTypes from 'prop-types';
import * as React from 'react';
import Qs from 'qs';
import moment from 'moment';

import Routes from 'routing/Routes';
import { DropdownButton, MenuItem } from 'components/graylog';
import naturalSort from 'javascript-natural-sort';
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';

class SurroundingSearchButton extends React.Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    timestamp: PropTypes.number.isRequired,
    searchConfig: PropTypes.object.isRequired,
    messageFields: PropTypes.object.isRequired,
  };

  _buildTimeRangeOptions = (searchConfig) => {
    const options = {};

    Object.keys(searchConfig.surrounding_timerange_options).forEach((key) => {
      options[moment.duration(key).asSeconds()] = searchConfig.surrounding_timerange_options[key];
    });

    return options;
  };

  _buildFilterFields = () => {
    const { messageFields, searchConfig } = this.props;
    const { surrounding_filter_fields: surroundingFilterFields = [] } = searchConfig;

    return surroundingFilterFields.reduce((prev, cur) => ({ ...prev, [cur]: messageFields[cur] }), {});
  };

  _buildSearchLink = (id, fromTime, toTime, fields, filter) => {
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

  _searchLink = (range) => {
    const { timestamp, id } = this.props;
    const fromTime = moment(timestamp).subtract(Number(range), 'seconds').toISOString();
    const toTime = moment(timestamp).add(Number(range), 'seconds').toISOString();
    const filterFields = this._buildFilterFields();

    return this._buildSearchLink(id, fromTime, toTime, [], filterFields);
  };

  render() {
    const timeRangeOptions = this._buildTimeRangeOptions(this.props.searchConfig);
    const menuItems = Object.keys(timeRangeOptions)
      .sort((a, b) => naturalSort(a, b))
      .map((key, idx) => {
        return (
          <MenuItem key={idx} href={this._searchLink(key)}>{timeRangeOptions[key]}</MenuItem>
        );
      });

    return (
      <DropdownButton title="Show surrounding messages" bsSize="small" id="surrounding-search-dropdown">
        {menuItems}
      </DropdownButton>
    );
  }
}

export default SurroundingSearchButton;
