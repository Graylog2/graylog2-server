// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { useField } from 'formik';

import AbsoluteTimeRangeSelector from 'views/components/searchbar/date-time-picker/AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from 'views/components/searchbar/date-time-picker/KeywordTimeRangeSelector';
import RelativeTimeRangeSelector from 'views/components/searchbar/date-time-picker/RelativeTimeRangeSelector';
import type { SearchesConfig } from 'components/search/SearchConfig';
import type { TimeRangeTypes } from 'views/logic/queries/Query';

import DisabledTimeRangeSelector from './DisabledTimeRangeSelector';

type Props = {
  disabled: boolean,
  config: SearchesConfig,
};

const timerangeStrategies = {
  absolute: {
    component: AbsoluteTimeRangeSelector,
  },
  relative: {
    component: RelativeTimeRangeSelector,
  },
  keyword: {
    component: KeywordTimeRangeSelector,
  },
};

const timerangeStrategy = (type: ?TimeRangeTypes) => {
  if (!type) {
    return { component: DisabledTimeRangeSelector };
  }

  return timerangeStrategies[type] || { component: DisabledTimeRangeSelector };
};

export default function TimeRangeInput({ disabled, config }: Props) {
  const [{ value }] = useField('timerange.type');
  const { component: Component } = timerangeStrategy(value);

  return <Component disabled={disabled} config={config} />;
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
