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
import type { DefaultTheme } from 'styled-components';

import Icon from 'components/common/Icon';

import type { HealthStatus } from './HealthReport.types';

type Props = {
  className?: string;
  size?: 'sm' | 'md' | 'lg';
  status: HealthStatus;
  title?: string;
};

const iconSize = {
  sm: 'sm',
  md: undefined,
  lg: 'lg',
} as const;

const iconName = {
  healthy: 'check_circle',
  warning: 'warning',
  critical: 'error',
  unknown: 'help',
} as const;

const getColor = (status: HealthStatus, theme: DefaultTheme) => {
  switch (status) {
    case 'healthy':
      return theme.colors.variant.success;
    case 'warning':
      return theme.colors.variant.warning;
    case 'critical':
      return theme.colors.variant.danger;
    case 'unknown':
    default:
      return theme.colors.gray[60];
  }
};

const StyledIcon = styled(Icon)<{ $status: HealthStatus }>(
  ({ $status }) => css`
    color: ${({ theme }) => getColor($status, theme)};
  `,
);

const HealthStatusIcon = ({ className = undefined, size = 'md', status, title = undefined }: Props) => (
  <StyledIcon
    className={className}
    name={iconName[status]}
    size={iconSize[size]}
    $status={status}
    title={title}
    aria-hidden={true}
  />
);

export default HealthStatusIcon;
