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
import { useContext, useMemo } from 'react';

import { isTypeRelativeWithEnd } from 'views/typeGuards/timeRange';
import { RELATIVE_ALL_TIME, DEFAULT_RELATIVE_FROM, DEFAULT_RELATIVE_TO } from 'views/Constants';
import { Icon } from 'components/common';
import TimerangeSelector from 'views/components/searchbar/TimerangeSelector';
import RangePresetDropdown from 'views/components/searchbar/RangePresetDropdown';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import TimeRangeInputSettingsContext from 'views/components/contexts/TimeRangeInputSettingsContext';

import { classifyToRange, classifyFromRange, RELATIVE_CLASSIFIED_ALL_TIME_RANGE } from './RelativeTimeRangeClassifiedHelper';
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
const ConfiguredWrapper = styled.div`
  grid-area: 3 / 5 / 3 / 7;
  margin: 3px 12px 3px 0;
  justify-self: end;
`;
const StyledIcon = styled(Icon)`
  flex: 0.75;
`;

const TabRelativeTimeRange = ({ disabled, limitDuration }: Props) => {
  const { values: { nextTimeRange }, setFieldValue } = useFormikContext<TimeRangeDropDownFormValues>();
  const disableUntil = disabled || (isTypeRelativeWithEnd(nextTimeRange) && nextTimeRange.from === RELATIVE_ALL_TIME);
  const { config } = useSearchConfiguration();
  const availableOptions = config?.quick_access_timerange_presets;
  const { showRelativePresetsButton } = useContext(TimeRangeInputSettingsContext);
  const relativeOptions = useMemo(() => availableOptions.filter((option) => option?.timerange?.type === 'relative'), [availableOptions]);

  const onSetPreset = (range) => {
    const isUnsetting = range === 0;

    if (isUnsetting) {
      setFieldValue('nextTimeRange.to', RELATIVE_CLASSIFIED_ALL_TIME_RANGE);
    }

    setFieldValue('nextTimeRange', classifyFromRange(range));
  };

  return (
    <RelativeWrapper>
      <>
        <RelativeRangeSelect classifyRange={(range) => classifyFromRange(range)}
                             defaultRange={classifyFromRange(DEFAULT_RELATIVE_FROM)}
                             disableUnsetRange={limitDuration !== 0}
                             disabled={disabled}
                             fieldName="from"
                             limitDuration={limitDuration}
                             onUnsetRange={() => { setFieldValue('nextTimeRange.to', RELATIVE_CLASSIFIED_ALL_TIME_RANGE); }}
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
      {showRelativePresetsButton
        && (
          <ConfiguredWrapper>
            <TimerangeSelector className="relative">
              <RangePresetDropdown disabled={disabled}
                                   onChange={onSetPreset}
                                   availableOptions={relativeOptions} />
            </TimerangeSelector>
          </ConfiguredWrapper>
        )}
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
