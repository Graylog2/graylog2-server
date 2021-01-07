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

import { AbsoluteTimeRange } from 'views/logic/queries/Query';

import AbsoluteDateInput from './AbsoluteDateInput';

type Props = {
  disabled: boolean,
  range: 'to' | 'from',
  currentTimeRange: AbsoluteTimeRange,
};

const ErrorMessage = styled.span(({ theme }) => css`
  color: ${theme.colors.variant.dark.danger};
  font-size: ${theme.fonts.size.tiny};
  font-style: italic;
  padding: 3px 3px 9px;
  height: 1.5em;
`);

const AbsoluteTimestamp = ({ disabled, currentTimeRange, range }: Props) => {
  return (
    <Field name={`nextTimeRange[${range}]`}>
      {({ field: { value, onChange, name }, meta: { error } }) => {
        const _onChange = (newValue) => onChange({ target: { name, value: newValue } });
        const dateTime = error ? currentTimeRange.from : value || currentTimeRange.from;

        return (
          <>
            <AbsoluteDateInput name={name}
                               disabled={disabled}
                               value={dateTime}
                               onChange={_onChange} />

            <ErrorMessage>{error}</ErrorMessage>
          </>
        );
      }}
    </Field>
  );
};

AbsoluteTimestamp.propTypes = {
  disabled: PropTypes.bool,
  currentTimeRange: PropTypes.shape({ from: PropTypes.string, to: PropTypes.string }).isRequired,
  range: PropTypes.oneOf(['to', 'from']).isRequired,
};

AbsoluteTimestamp.defaultProps = {
  disabled: false,
};

export default AbsoluteTimestamp;
