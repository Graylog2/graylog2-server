import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';

import Input from 'components/bootstrap/Input';

export default function RelativeTimeRangeSelector({ config, value, onChange }) {
  const availableOptions = config.relative_timerange_options;
  const timeRangeLimit = moment.duration(config.query_time_range_limit);
  let options;

  if (availableOptions) {
    let all = null;
    options = Object.keys(availableOptions).map((key) => {
      const seconds = moment.duration(key).asSeconds();

      if (timeRangeLimit > 0 && (seconds > timeRangeLimit.asSeconds() || seconds === 0)) {
        return null;
      }

      const option = (<option key={`relative-option-${key}`} value={seconds}>{availableOptions[key]}</option>);

      // The "search in all messages" option should be the last one.
      if (key === 'PT0S') {
        all = option;
        return null;
      }
      return option;
    });

    if (all) {
      options.push(all);
    }
  } else {
    options = (<option value="300">Loading...</option>);
  }

  return (
    <div className="timerange-selector relative"
         style={{ width: 270, marginLeft: 50 }}>
      <Input id="relative-timerange-selector"
             type="select"
             value={value.relative}
             name="relative"
             onChange={event => onChange('relative', event.target.value)}
             className="input-sm">
        {options}
      </Input>
    </div>
  );
}

RelativeTimeRangeSelector.propTypes = {
  config: PropTypes.shape({
    relative_timerange_options: PropTypes.object.isRequired,
    query_time_range_limit: PropTypes.object.isRequired,
  }).isRequired,
  value: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
};
