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

import type { QuickAccessTimeRange } from 'components/configurations/QuickAccessTimeRangeForm';
import type {
  AbsoluteTimeRange,
  KeywordTimeRange,
  TimeRange,
} from 'views/logic/queries/Query';
import { dateOutput } from 'views/components/searchbar/TimeRangeDisplay';

type Props = { options : Array<QuickAccessTimeRange>};

const getTimeRangeValueSummary = (timerange: TimeRange) => {
  switch (timerange.type) {
    case 'relative':
      return `${dateOutput(timerange).from} - ${dateOutput(timerange).until}`;
    case 'absolute':
      return `${(timerange as AbsoluteTimeRange).from} - ${(timerange as AbsoluteTimeRange).to}`;
    case 'keyword':
      return (timerange as KeywordTimeRange).keyword;
    default:
      throw Error('Timerange type doesn\'t not exist');
  }
};

const QuickAccessTimeRangeOptionsSummary = ({ options }: Props) => (
  <dl className="deflist">
    {options.map(({ timerange, id, description }) => (
      <span key={`timerange-options-summary-${id}`}>
        <dt>{description}</dt>
        <dd>{getTimeRangeValueSummary(timerange)}</dd>
      </span>
    ))}
  </dl>
);

QuickAccessTimeRangeOptionsSummary.propTypes = {
  options: PropTypes.object.isRequired,
};

export default QuickAccessTimeRangeOptionsSummary;
