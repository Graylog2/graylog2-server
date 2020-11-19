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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { useField } from 'formik';

import type { SearchesConfig } from 'components/search/SearchConfig';
import AbsoluteTimeRangeSelector from 'views/components/searchbar/AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from 'views/components/searchbar/KeywordTimeRangeSelector';
import RelativeTimeRangeSelector from 'views/components/searchbar/RelativeTimeRangeSelector';
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
