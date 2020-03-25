// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { useField } from 'formik';

import type { SearchesConfig } from 'components/search/SearchConfig';
import DateTime from 'logic/datetimes/DateTime';
import AbsoluteTimeRangeSelector from 'views/components/searchbar/AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from 'views/components/searchbar/KeywordTimeRangeSelector';
import RelativeTimeRangeSelector from 'views/components/searchbar/RelativeTimeRangeSelector';

type Props = {
  disabled: boolean,
  config: SearchesConfig,
};

const _isValidDateString = (dateString: string) => {
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

export default function TimeRangeInput({ disabled, config }: Props) {
  const [{ value: timerange }] = useField('timerange');
  const { component: Component } = timerangeStrategies[timerange.type];
  return <Component disabled={disabled} timerange={timerange} config={config} />;
}

TimeRangeInput.propTypes = {
  config: PropTypes.shape({
    relative_timerange_options: PropTypes.objectOf(PropTypes.string).isRequired,
  }).isRequired,
  disabled: PropTypes.bool,
};

TimeRangeInput.defaultProps = {
  disabled: false,
};
