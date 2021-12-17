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
import styled, { css } from 'styled-components';
import { Field } from 'formik';

import type { AbsoluteTimeRange } from 'views/logic/queries/Query';

import AbsoluteDatePicker from './AbsoluteDatePicker';
import AbsoluteTimeInput from './AbsoluteTimeInput';

type Props = {
  startDate?: Date,
  range: 'to' | 'from',
  nextTimeRange: AbsoluteTimeRange,
};

const ErrorMessage = styled.span(({ theme }) => css`
  color: ${theme.colors.variant.dark.danger};
  font-size: ${theme.fonts.size.small};
  font-style: italic;
  padding: 3px 3px 9px;
  height: 1.5em;
`);

const AbsoluteCalendar = ({ startDate, nextTimeRange, range }: Props) => {
  return (
    <Field name={`nextTimeRange[${range}]`}>
      {({ field: { value, onChange, name }, meta: { error } }) => {
        const _onChange = (newValue) => onChange({ target: { name, value: newValue } });
        const dateTime = error ? nextTimeRange[range] : value || nextTimeRange[range];

        return (
          <>
            <AbsoluteDatePicker onChange={_onChange}
                                startDate={startDate}
                                dateTime={dateTime} />

            <AbsoluteTimeInput onChange={_onChange}
                               range={range}
                               dateTime={dateTime} />

            <ErrorMessage>{error}</ErrorMessage>
          </>
        );
      }}
    </Field>
  );
};

AbsoluteCalendar.propTypes = {
  nextTimeRange: PropTypes.shape({ from: PropTypes.string, to: PropTypes.string }).isRequired,
  startDate: PropTypes.instanceOf(Date),
  range: PropTypes.oneOf(['to', 'from']).isRequired,
};

AbsoluteCalendar.defaultProps = {
  startDate: undefined,
};

export default AbsoluteCalendar;
