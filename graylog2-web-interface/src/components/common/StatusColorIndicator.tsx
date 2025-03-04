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
import type { ColorVariant } from '@graylog/sawmill';

import Indicator from 'components/bootstrap/Indicator';

type Props = React.PropsWithChildren<{
  bsStyle?: ColorVariant;
  'data-testid'?: string;
}>;
const StyledIndicator = styled(Indicator)<{ color: ColorVariant }>(
  ({ color, theme }) => css`
  .mantine-Indicator-indicator::before {
      ${color === 'gray' && css`background-color: ${theme.colors.gray[50]}`}
    }
  `,
);


const StatusColorIndicator = ({ bsStyle = 'gray', 'data-testid': dataTestid, children = undefined }: Props) => (
  <StyledIndicator
    color={bsStyle}
    radius={0}
    size={8}
    data-testid={dataTestid}>
    {children}
  </StyledIndicator>
);

export default StatusColorIndicator;
