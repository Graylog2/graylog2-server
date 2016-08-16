import React from 'react';
import { DropdownButton, MenuItem } from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';

import StoreProvider from 'injection/StoreProvider';
const SearchStore = StoreProvider.getStore('Search');

import moment from 'moment';

const SurroundingSearchButton = React.createClass({
  propTypes: {
    id: React.PropTypes.string.isRequired,
    timestamp: React.PropTypes.number.isRequired,
    searchConfig: React.PropTypes.object.isRequired,
    messageFields: React.PropTypes.object.isRequired,
  },

  _buildTimeRangeOptions(searchConfig) {
    const options = {};

    Object.keys(searchConfig.surrounding_timerange_options).forEach((key) => {
      options[moment.duration(key).asSeconds()] = searchConfig.surrounding_timerange_options[key];
    });

    return options;
  },

  _buildFilterFields() {
    const fields = {};

    if (this.props.searchConfig) {
      this.props.searchConfig.surrounding_filter_fields.forEach((field) => {
        fields[field] = this.props.messageFields[field];
      });
    }

    return fields;
  },

  _searchLink(range) {
    const fromTime = moment.unix(this.props.timestamp - Number(range)).toISOString();
    const toTime = moment.unix(this.props.timestamp + Number(range)).toISOString();

    return SearchStore.searchSurroundingMessages(this.props.id, fromTime, toTime, this._buildFilterFields());
  },

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
  },
});

export default SurroundingSearchButton;
