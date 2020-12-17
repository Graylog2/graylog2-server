/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { Field } from 'formik';
import styled from 'styled-components';

import Input from 'components/bootstrap/Input';
import useSearchConfiguration from 'hooks/useSearchConfiguration';

import TimerangeSelector from '../TimerangeSelector';

type Props = {
  disabled: boolean,
  onChange: (range: number) => void,
};
const StyledInput = styled(Input)`
  margin-bottom: 10px;
`;
const StyledTimerangeSelector = styled(TimerangeSelector)`
  flex-basis: 100%;
`;

export default function ConfiguredRelativeTimeRangeSelector({ disabled, onChange }: Props) {
  const { config } = useSearchConfiguration();
  const availableOptions = config?.relative_timerange_options;
  const timeRangeLimit = moment.duration(config?.query_time_range_limit);
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
    <Field name="timerange-existing-range">
      {({ field: { name, value } }) => {
        const _onChange = (e) => {
          const { target: { value: newValue } } = e;

          onChange(Number.parseInt(newValue, 10));
        };

        return (
          <StyledTimerangeSelector className="relative">
            <StyledInput id="relative-timerange-selector"
                         disabled={disabled}
                         type="select"
                         label="Choose existing relative time range:"
                         value={value}
                         title="Select a relative time range"
                         className="relative"
                         name={name}
                         onChange={_onChange}>
              {options}
            </StyledInput>
          </StyledTimerangeSelector>
        );
      }}
    </Field>
  );
}

ConfiguredRelativeTimeRangeSelector.propTypes = {
  onChange: PropTypes.func,
  disabled: PropTypes.bool,
};

ConfiguredRelativeTimeRangeSelector.defaultProps = {
  disabled: false,
  onChange: () => {},
};
