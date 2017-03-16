import React from 'react';
import moment from 'moment';
import jQuery from 'jquery';

import Select from 'components/common/Select';

const TimezoneSelect = React.createClass({
  propTypes: {
    onChange: React.PropTypes.func,
  },

  getValue() {
    return this.refs.timezone.getValue();
  },

  // Some time zones are not stored into any areas, this is the group we use to put them apart in the dropdown
  // https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
  _UNCLASSIFIED_AREA: 'Unclassified',

  _formatTimezones() {
    const timezones = {};
    moment.tz.names().forEach((timezone) => {
      const splitted = timezone.split('/');
      const area = (splitted.length > 1 ? splitted[0] : this._UNCLASSIFIED_AREA);
      const location = (splitted.length > 1 ? splitted[1] : splitted[0]);

      if (!timezones[area]) {
        timezones[area] = [];
      }

      timezones[area].push(location);
    });

    return [].concat.apply([], Object.keys(timezones).sort().map((area) => {
      return [{ label: area, disabled: true, value: area }]
        .concat(jQuery.unique(timezones[area])
          .sort()
          .map((location) => {
            const timezone = (area === this._UNCLASSIFIED_AREA ? location : `${area}/${location}`);
            return { value: timezone, label: location.replace('_', ' ') };
          }),
        );
    }));
  },
  _renderOption(option) {
    if (!option.disabled) {
      return <span key={option.value} title={option.value}>&nbsp; {option.label}</span>;
    }
    return <span key={option.value} title={option.value}>{option.label}</span>;
  },
  render() {
    const timezones = this._formatTimezones();
    return (
      <Select ref="timezone" {...this.props}
              placeholder="Pick a time zone"
              options={timezones}
              optionRenderer={this._renderOption} />
    );
  },
});

export default TimezoneSelect;
