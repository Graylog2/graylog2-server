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
import styled from 'styled-components';
import { useFormikContext } from 'formik';

import { isTypeRelativeWithEnd } from 'views/typeGuards/timeRange';
import { RELATIVE_ALL_TIME, DEFAULT_RELATIVE_FROM, DEFAULT_RELATIVE_TO } from 'views/Constants';
import { Icon } from 'components/common';

import { classifyToRange, classifyFromRange } from './RelativeTimeRangeClassifiedHelper';
import type { TimeRangeDropDownFormValues } from './TimeRangeDropdown';
import RelativeRangeSelect from './RelativeRangeSelect';

type Props = {
  disabled: boolean,
  limitDuration: number,
};

const RelativeWrapper = styled.div`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: space-around;
`;

const StyledIcon = styled(Icon)`
  flex: 0.75;
`;

const TabRelativeTimeRange = ({ disabled, limitDuration }: Props) => {
  const { values: { nextTimeRange } } = useFormikContext<TimeRangeDropDownFormValues>();
  const disableUntil = disabled || (isTypeRelativeWithEnd(nextTimeRange) && nextTimeRange.from === RELATIVE_ALL_TIME);

  return (
    <RelativeWrapper>
      <>
        <RelativeRangeSelect classifyRange={(range) => classifyFromRange(range)}
                             defaultRange={classifyFromRange(DEFAULT_RELATIVE_FROM)}
                             disableUnsetRange={limitDuration !== 0}
                             disabled={disabled}
                             fieldName="from"
                             limitDuration={limitDuration}
                             title="From:"
                             unsetRangeLabel="All Time"
                             unsetRangeValue={0} />
        <StyledIcon name="arrow-right" />

        <RelativeRangeSelect classifyRange={(range) => classifyToRange(range)}
                             defaultRange={classifyToRange(DEFAULT_RELATIVE_TO)}
                             disableUnsetRange={disableUntil}
                             disabled={disableUntil}
                             fieldName="to"
                             limitDuration={limitDuration}
                             title="Until:"
                             unsetRangeLabel="Now"
                             unsetRangeValue={undefined} />
      </>
    </RelativeWrapper>
  );
};

TabRelativeTimeRange.propTypes = {
  limitDuration: PropTypes.number,
  disabled: PropTypes.bool,
};

TabRelativeTimeRange.defaultProps = {
  disabled: false,
  limitDuration: 0,
};

export default TabRelativeTimeRange;
