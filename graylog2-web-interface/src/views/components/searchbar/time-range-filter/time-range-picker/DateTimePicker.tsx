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
import styled, { css } from 'styled-components';

import AbsoluteDatePicker from 'views/components/searchbar/time-range-filter/time-range-picker/AbsoluteDatePicker';
import AbsoluteTimeInput from 'views/components/searchbar/time-range-filter/time-range-picker/AbsoluteTimeInput';

const ErrorMessage = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.variant.dark.danger};
    font-size: ${theme.fonts.size.small};
    font-style: italic;
    padding: 3px 3px 9px;
    height: 1.5em;
  `,
);

const Overlay = styled.div`
  opacity: 0.1;
`;
const Disabled = ({ disabled, children = undefined }: React.PropsWithChildren<{ disabled: boolean }>) =>
  disabled ? (
    <Overlay>
      <div inert="">{children}</div>
    </Overlay>
  ) : (
    children
  );

type Props = {
  disabled?: boolean;
  error: string;
  onChange: (newValue: string) => void;
  startDate?: Date;
  value: string;
  range?: string;
};

const DateTimePicker = ({
  disabled = false,
  error,
  onChange,
  startDate = undefined,
  value,
  range = 'Range',
}: Props) => (
  <>
    <Disabled disabled={disabled}>
      <AbsoluteDatePicker onChange={onChange} startDate={startDate} dateTime={value} />

      <AbsoluteTimeInput onChange={onChange} range={range} dateTime={value} />
    </Disabled>

    <ErrorMessage>{error}</ErrorMessage>
  </>
);

export default DateTimePicker;
