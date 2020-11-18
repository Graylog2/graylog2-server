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
import PropTypes from 'prop-types';
import React from 'react';
import moment from 'moment-timezone';
import lodash from 'lodash';

import Select from 'components/common/Select';

/**
 * Component that renders a select input for all supported time zones.
 *
 * As this component is based in the `Select` component, users can search a
 * certain time zone easily. This component will pass through other props
 * to the underlying `Select` component, so you can further customize how
 * the `Select` input behaves. Check the `Select` documentation for more
 * information.
 */
class TimezoneSelect extends React.Component {
  static propTypes = {
    /**
     * Function that will be called when the selected timezone changes. The
     * function will receive the new time zone identifier as argument. See
     * https://en.wikipedia.org/wiki/List_of_tz_database_time_zones for
     * a list of time zone identifiers.
     */
    onChange: PropTypes.func,
  };

  static defaultProps = {
    onChange: () => {},
  }

  // Some time zones are not stored into any areas, this is the group we use to put them apart in the dropdown
  // https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
  _UNCLASSIFIED_AREA = 'Unclassified';

  getValue = () => {
    return this.timezone.getValue();
  };

  _formatTimezones = () => {
    const timezones = {};

    // Group time zones by area
    moment.tz.names().forEach((timezone) => {
      const splitted = timezone.split('/');
      const area = (splitted.length > 1 ? splitted[0] : this._UNCLASSIFIED_AREA);
      const location = (splitted.length > 1 ? splitted[1] : splitted[0]);

      if (!timezones[area]) {
        timezones[area] = [];
      }

      timezones[area].push(location);
    });

    const labels = [];

    Object.keys(timezones)
      .sort()
      .forEach((area) => {
        // Add disabled area option to use as TZ separator
        labels.push({ label: area, disabled: true, value: area });

        // Now add a label per timezone in the area
        const effectiveTimezones = lodash.uniq(timezones[area]).sort();
        const timezoneLabels = effectiveTimezones.map((location) => {
          const timezone = (area === this._UNCLASSIFIED_AREA ? location : `${area}/${location}`);

          return { value: timezone, label: location.replace('_', ' ') };
        });

        labels.push(...timezoneLabels);
      });

    return labels;
  };

  _renderOption = (option) => {
    if (!option.disabled) {
      return <span key={option.value} title={option.value}>&nbsp; {option.label}</span>;
    }

    return <span key={option.value} title={option.value}>{option.label}</span>;
  };

  render() {
    const timezones = this._formatTimezones();
    const { onChange, ...otherProps } = this.props;

    return (
      <Select ref={(timezone) => { this.timezone = timezone; }}
              {...otherProps}
              inputId="timezone-select"
              onChange={onChange}
              placeholder="Pick a time zone"
              options={timezones}
              optionRenderer={this._renderOption} />
    );
  }
}

export default TimezoneSelect;
