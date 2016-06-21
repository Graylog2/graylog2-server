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
  _formatTimezones() {
    const timezones = {};
    moment.tz.names().forEach((timezone) => {
      const splitted = timezone.split('/');
      const area = (splitted.length > 1 ? splitted[0] : 'Etc');
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
            return { value: `${area}/${location}`, label: location.replace('_', ' ') };
          })
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
