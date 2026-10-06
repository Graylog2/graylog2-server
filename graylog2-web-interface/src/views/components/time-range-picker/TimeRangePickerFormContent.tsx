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

import { Col, Row } from 'components/bootstrap';
import useUserDateTime from 'hooks/useUserDateTime';

import TimeRangeTabs, { timeRangePickerTabs } from './TimeRangePickerTabs';
import TimeRangePresetRow from './TimeRangePresetRow';
import type { SupportedTimeRangeType } from './types';

export const allTimeRangeTypes = Object.keys(timeRangePickerTabs) as Array<SupportedTimeRangeType>;

const Timezone = styled.p(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    padding-left: 3px;
    margin: 0;
    min-height: 34px;
    display: flex;
    align-items: center;
  `,
);

type Props = React.PropsWithChildren<{
  limitDuration: number;
  validTypes?: Array<SupportedTimeRangeType>;
}>;

const TimeRangePickerFormContent = ({ children = undefined, validTypes = allTimeRangeTypes, limitDuration }: Props) => {
  const { userTimezone } = useUserDateTime();

  return (
    <>
      <Row>
        <Col md={12}>
          <TimeRangePresetRow limitDuration={limitDuration} />
          <TimeRangeTabs limitDuration={limitDuration} validTypes={validTypes} />
        </Col>
      </Row>

      <Row className="row-sm">
        <Col md={6}>
          <Timezone>
            All timezones using: <b>{userTimezone}</b>
          </Timezone>
        </Col>
        <Col md={6}>{children}</Col>
      </Row>
    </>
  );
};

export default TimeRangePickerFormContent;
