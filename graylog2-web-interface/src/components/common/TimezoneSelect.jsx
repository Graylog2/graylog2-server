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
      const continent = (splitted.length > 1 ? splitted[0] : 'Etc');
      const city = (splitted.length > 1 ? splitted[1] : splitted[0]);

      if (!timezones[continent]) {
        timezones[continent] = [];
      }

      timezones[continent].push(city);
    });

    return [].concat.apply([], Object.keys(timezones).sort().map((continent) => {
      return [{label: continent, disabled: true, value: continent}]
        .concat(jQuery.unique(timezones[continent])
          .sort()
          .map((timezone) => { return {value: continent + '/' + timezone, label: timezone.replace("_", " ")}; })
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
              placeholder="Pick your time zone"
              options={timezones}
              optionRenderer={this._renderOption}/>
    );
  },
});

export default TimezoneSelect;
