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
import styled from 'styled-components';
import { TimeRange } from 'src/views/logic/queries/Query';

import { RELATIVE_RANGE_TYPES, DEFAULT_TIMERANGE, DEFAULT_OFFSET_RANGE } from 'views/Constants';
import { Icon } from 'components/common';

import RelativeTimeRangeField from './RelativeTimeRangeField';

type Props = {
  disabled: boolean,
  originalTimeRange: TimeRange,
  limitDuration: number,
};

const RelativeWrapper = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-around;
`;

const StyledIcon = styled(Icon)`
  flex: 0.75;
`;

const buildRangeTypes = (limitDuration) => RELATIVE_RANGE_TYPES.map(({ label, type }) => {
  const typeDuration = moment.duration(1, type).asSeconds();

  if (limitDuration === 0 || typeDuration <= limitDuration) {
    return { label, value: type };
  }

  return null;
}).filter(Boolean);

const getDefaultRange = (originalTimeRange) => {
  if ('range' in originalTimeRange && originalTimeRange.range) {
    return originalTimeRange.range;
  }

  return DEFAULT_TIMERANGE.range;
};

const getDefaultOffset = (originalTimeRange) => {
  if ('offset' in originalTimeRange && originalTimeRange.offset) {
    return originalTimeRange.offset;
  }

  return DEFAULT_OFFSET_RANGE;
};

const RelativeTimeRangeSelector = ({ disabled, originalTimeRange, limitDuration }: Props) => {
  const availableRangeTypes = buildRangeTypes(limitDuration);
  const defaultRange = getDefaultRange(originalTimeRange);
  const defaultOffset = getDefaultOffset(originalTimeRange);

  return (
    <RelativeWrapper>
      <RelativeTimeRangeField originalTimeRange={originalTimeRange}
                              name="nextTimeRange.range"
                              availableRangeTypes={availableRangeTypes}
                              disabled={disabled}
                              unsetRangeLabel="All Time"
                              limitDuration={limitDuration}
                              defaultRange={defaultRange}
                              title="From:" />

      <StyledIcon name="arrow-right" />

      <RelativeTimeRangeField originalTimeRange={originalTimeRange}
                              name="nextTimeRange.offset"
                              availableRangeTypes={availableRangeTypes}
                              defaultRange={defaultOffset}
                              disabled={disabled}
                              unsetRangeLabel="Now"
                              limitDuration={limitDuration}
                              title="Until:" />
    </RelativeWrapper>
  );
};

RelativeTimeRangeSelector.propTypes = {
  limitDuration: PropTypes.number,
  disabled: PropTypes.bool,
  originalTimeRange: PropTypes.shape({
    range: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  }).isRequired,
};

RelativeTimeRangeSelector.defaultProps = {
  disabled: false,
  limitDuration: 0,
};

export default RelativeTimeRangeSelector;
