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
import { Field } from 'formik';
import styled, { css } from 'styled-components';
import moment from 'moment';

import { TimeRange, AbsoluteTimeRange } from 'views/logic/queries/Query';

import AbsoluteDateInput from './AbsoluteDateInput';
import AbsoluteDatePicker from './AbsoluteDatePicker';
import AbsoluteTimeInput from './AbsoluteTimeInput';

type Props = {
  disabled: boolean,
  from: boolean,
  currentTimeRange?: AbsoluteTimeRange,
  originalTimeRange: TimeRange,
  limitDuration?: number,
};

const ErrorMessage = styled.span(({ theme }) => css`
  color: ${theme.colors.variant.dark.danger};
  font-size: ${theme.fonts.size.tiny};
  font-style: italic;
  padding: 3px 3px 9px;
  height: 1.5em;
`);

const AbsoluteRangeField = ({ disabled, limitDuration, from, currentTimeRange }: Props) => {
  const range = from ? 'from' : 'to';

  return (
    <Field name={`nextTimeRange[${range}]`}>
      {({ field: { value, onChange, name }, meta: { error } }) => {
        const _onChange = (newValue) => onChange({ target: { name, value: newValue } });
        const dateTime = error ? currentTimeRange[range] : value || currentTimeRange[range];
        let startDate = moment(currentTimeRange.from).toDate();

        if (from) {
          startDate = limitDuration ? moment().seconds(-limitDuration).toDate() : startDate;
        }

        return (
          <>
            <AbsoluteDateInput name={name}
                               disabled={disabled}
                               value={value}
                               onChange={_onChange} />

            <AbsoluteDatePicker name={name}
                                disabled={disabled}
                                value={value}
                                onChange={_onChange}
                                startDate={startDate}
                                dateTime={dateTime} />

            <AbsoluteTimeInput onChange={_onChange}
                               range={range}
                               dateTime={dateTime} />

            <ErrorMessage>{error ?? ' '}</ErrorMessage>
          </>
        );
      }}
    </Field>
  );
};

AbsoluteRangeField.propTypes = {
  from: PropTypes.bool.isRequired,
  currentTimeRange: PropTypes.shape({
    from: PropTypes.string,
    to: PropTypes.string,
  }),
  originalTimeRange: PropTypes.shape({
    from: PropTypes.string,
    to: PropTypes.string,
  }).isRequired,
  disabled: PropTypes.bool,
  limitDuration: PropTypes.number,
};

AbsoluteRangeField.defaultProps = {
  disabled: false,
  limitDuration: 0,
  currentTimeRange: undefined,
};

export default AbsoluteRangeField;
