import React from 'react';
import Reflux from 'reflux';

import TimezonesStore from 'stores/system/TimezonesStore';

import Select from 'components/common/Select';
import Spinner from 'components/common/Spinner';

const TimezoneSelect = React.createClass({
  mixins: [Reflux.connect(TimezonesStore)],
  getValue() {
    return this.refs.timezone.getValue();
  },
  _formatTimezones(timezones) {
    /*return Object.keys(timezones).sort().map((continent) => {
      return (
        <optgroup key={'timezone-group-' + continent} label={continent}>
          {timezones[continent].sort().map((timezone) => <option key={'timezone-' + continent + '-' + timezone} value={continent + '/' + timezone}>{timezone}</option>)}
        </optgroup>
      );
    });*/
    return [].concat.apply([], Object.keys(timezones).sort().map((continent) => {
      return [{label: continent, disabled: true, value: continent}]
        .concat(timezones[continent]
          .sort()
          .map((timezone) => { return {value: continent + '/' + timezone, label: timezone.replace("_", " ")}; })
        );
    }));
  },
  _renderOption(option) {
    if (!option.disabled) {
      return <span key={option.value}>&nbsp; {option.label}</span>;
    }
    return <span key={option.value}>{option.label}</span>;
  },
  render() {
    if (!this.state.timezones) {
      return <Spinner />;
    }

    return (
      <Select ref="timezone" {...this.props}
              placeholder="Pick your time zone"
              options={this._formatTimezones(this.state.timezones)}
              optionRenderer={this._renderOption}/>
    );
  },
});

export default TimezoneSelect;
