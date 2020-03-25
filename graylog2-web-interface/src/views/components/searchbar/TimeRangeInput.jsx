// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { useField } from 'formik';

import type { SearchesConfig } from 'components/search/SearchConfig';
import DateTime from 'logic/datetimes/DateTime';
import AbsoluteTimeRangeSelector from 'views/components/searchbar/AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from 'views/components/searchbar/KeywordTimeRangeSelector';
import RelativeTimeRangeSelector from 'views/components/searchbar/RelativeTimeRangeSelector';
import type { TimeRange } from 'views/logic/queries/Query';

type Props = {
  disabled: boolean,
  timerange: TimeRange,
  config: SearchesConfig,
};

const _isValidDateString = (dateString: string) => {
  console.log('Validating field: ', dateString);
  return (DateTime.isValidDateString(dateString)
    ? undefined
    : `Invalid date: ${dateString}`);
};

const _validateAbsoluteTimerange = (timerange) => {
  const fromError = _isValidDateString(timerange.from);
  const toError = _isValidDateString(timerange.to);
  const result = {};

  if (fromError) {
    result.from = fromError;
  }
  if (toError) {
    result.to = toError;
  }

  return result;
};

const timerangeStrategies = {
  absolute: {
    component: AbsoluteTimeRangeSelector,
    validate: _validateAbsoluteTimerange,
  },
  relative: {
    component: RelativeTimeRangeSelector,
    validate: () => ({}),
  },
  keyword: {
    component: KeywordTimeRangeSelector,
    validate: () => ({}),
  },
};

const _validateTimerange = (timerange) => {
  const { validate } = timerangeStrategies[timerange.type];
  const timerangeErrors = validate(timerange);
  return Object.keys(timerangeErrors).length > 0 ? timerangeErrors : undefined;
};

export default function TimeRangeInput({ disabled, timerange, config }: Props) {
  const { component: Component } = timerangeStrategies[timerange.type];
  const [{ value }] = useField('timerange');
  return <Component disabled={disabled} timerange={value} config={config} />;
}

TimeRangeInput.propTypes = {
  config: PropTypes.shape({
    relative_timerange_options: PropTypes.objectOf(PropTypes.string).isRequired,
  }).isRequired,
  disabled: PropTypes.bool,
  timerange: PropTypes.object.isRequired,
};

TimeRangeInput.defaultProps = {
  disabled: false,
};
