import React from 'react';

import Select from 'components/common/Select';

const TimezoneSelect = React.createClass({
  getValue() {
    return this.refs.timezone.getValue();
  },
  options: [
    { value: 'Europe', label: 'Europe' },
    { value: 'America', label: 'America' },
  ],
  render() {
    return (
      <Select ref="timezone" {...this.props} placeholder="Pick your time zone" options={this.options} />
    );
  },
});

export default TimezoneSelect;
