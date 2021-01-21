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

import { DEFAULT_RELATIVE_FROM, DEFAULT_RELATIVE_TO } from 'views/Constants';
import { Icon } from 'components/common';

import RelativeRangeSelect from './RelativeRangeSelect';
import type { TimeRangeDropDownFormValues } from './TimeRangeDropdown';

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

const _handleToChange = (
  formValues: TimeRangeDropDownFormValues,
  setFormValues: (TimeRangeDropDownFormValues) => void,
  toRange: number,
  updateField: () => void,
) => {
  const { nextTimeRange, ...otherFormValues } = formValues;

  const setNextTimeRange = (newTimeRange) => setFormValues({
    ...otherFormValues,
    nextTimeRange: newTimeRange,
  });

  if (!toRange && 'from' in nextTimeRange) {
    setNextTimeRange({
      type: nextTimeRange.type,
      range: nextTimeRange.from,
    });

    return;
  }

  if (toRange && 'range' in nextTimeRange) {
    setNextTimeRange({
      type: nextTimeRange.type,
      from: nextTimeRange.range,
      to: toRange,
    });

    return;
  }

  updateField();
};

const TabRelativeTimeRange = ({ disabled, limitDuration }: Props) => {
  const { setValues, values: formValues } = useFormikContext<TimeRangeDropDownFormValues>();
  const fromFieldName = 'from' in formValues.nextTimeRange ? 'from' : 'range';

  return (
    <RelativeWrapper>
      <RelativeRangeSelect disabled={disabled}
                           title="From:"
                           limitDuration={limitDuration}
                           unsetRangeLabel="All Time"
                           disableUnsetRange={limitDuration !== 0}
                           defaultRange={DEFAULT_RELATIVE_FROM}
                           fieldName={fromFieldName} />
      <StyledIcon name="arrow-right" />

      <RelativeRangeSelect disabled={disabled}
                           limitDuration={limitDuration}
                           defaultRange={DEFAULT_RELATIVE_TO}
                           handleOnChange={(toRange, updateField) => _handleToChange(formValues, setValues, toRange, updateField)}
                           title="Until:"
                           unsetRangeLabel="Now"
                           fieldName="to" />
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
