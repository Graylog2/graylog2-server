// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { Field } from 'formik';

import Input from 'components/bootstrap/Input';
import type { SearchesConfig } from 'components/search/SearchConfig';

import TimerangeSelector from './TimerangeSelector';

type Props = {
  disabled: boolean,
  config: SearchesConfig,
};

export default function RelativeTimeRangeSelector({ config, disabled }: Props) {
  const availableOptions = config.relative_timerange_options;
  const timeRangeLimit = moment.duration(config.query_time_range_limit);
  let options;

  if (availableOptions) {
    let all = null;

    options = Object.keys(availableOptions).map((key) => {
      const seconds = moment.duration(key).asSeconds();

      if (timeRangeLimit.seconds() > 0 && (seconds > timeRangeLimit.asSeconds() || seconds === 0)) {
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
    <Field name="tempTimeRange.range">
      {({ field: { name, value, onChange } }) => {
        const _onChange = (e) => {
          const { target: { value: newValue } } = e;

          onChange({ target: { name, value: Number.parseInt(newValue, 10) } });
        };

        return (
          <TimerangeSelector className="relative">
            <Input id="relative-timerange-selector"
                   disabled={disabled}
                   type="select"
                   value={value}
                   title="Select a relative time range"
                   className="relative"
                   name={name}
                   onChange={_onChange}>
              {options}
            </Input>
          </TimerangeSelector>
        );
      }}
    </Field>
  );
}

RelativeTimeRangeSelector.propTypes = {
  config: PropTypes.shape({
    relative_timerange_options: PropTypes.object.isRequired,
    query_time_range_limit: PropTypes.string.isRequired,
  }).isRequired,
  disabled: PropTypes.bool,
};

RelativeTimeRangeSelector.defaultProps = {
  disabled: false,
};
