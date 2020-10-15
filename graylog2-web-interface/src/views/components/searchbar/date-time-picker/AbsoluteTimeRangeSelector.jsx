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
import { Field } from 'formik';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

import DateTime from 'logic/datetimes/DateTime';
import { Icon } from 'components/common';
// import type { ThemeInterface } from 'theme';
import DateInputWithPicker from 'views/components/searchbar/DateInputWithPicker';

type Props = {
  disabled: boolean,
};

const AbsoluteWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-around;
`;

const RangeWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  flex: 4;
  align-items: center;
`;

const StyledIcon: StyledComponent<{}, void, typeof Icon> = styled(Icon)`
  flex: 0.75;
`;

const _isValidDateString = (dateString: string) => {
  if (dateString === undefined) {
    return undefined;
  }

  return DateTime.isValidDateString(dateString)
    ? undefined
    : 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]]';
};

const AbsoluteTimeRangeSelector = ({ disabled }: Props) => {
  return (
    <AbsoluteWrapper>
      <RangeWrapper>
        <Field name="tempTimeRange.from" validate={_isValidDateString}>
          {({ field: { value, onChange, onBlur, name }, meta: { error } }) => (
            <DateInputWithPicker disabled={disabled}
                                 onChange={onChange}
                                 onBlur={onBlur}
                                 value={value}
                                 name={name}
                                 title="Search start date"
                                 error={error} />
          )}
        </Field>
      </RangeWrapper>

      <StyledIcon name="arrow-right" />

      <RangeWrapper>
        <Field name="tempTimeRange.to" validate={_isValidDateString}>
          {({ field: { value, onChange, onBlur, name }, meta: { error } }) => (
            <DateInputWithPicker disabled={disabled}
                                 onChange={onChange}
                                 onBlur={onBlur}
                                 value={value}
                                 name={name}
                                 title="Search end date"
                                 error={error} />
          )}
        </Field>
      </RangeWrapper>
    </AbsoluteWrapper>
  );
};

AbsoluteTimeRangeSelector.propTypes = {
  disabled: PropTypes.bool,
};

AbsoluteTimeRangeSelector.defaultProps = {
  disabled: false,
};

export default AbsoluteTimeRangeSelector;
