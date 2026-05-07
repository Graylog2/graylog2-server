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
import type { ColorVariant } from '@graylog/sawmill';
import { Badge as MantineBadge } from '@mantine/core';
import styled, { css, useTheme } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

import type { BsSize } from 'components/bootstrap/types';
import sizeForMantine from 'theme/utils/sizeForMantine';
import type { SupportedMantineSize } from 'theme/types';

const mapStyle = (style: ColorVariant, theme: DefaultTheme) =>
  style === 'default' ? theme.colors.button.gray.background : theme.colors.variant[style];

const mapFontSize: Record<SupportedMantineSize, 'tiny' | 'small' | 'body'> = {
  xs: 'tiny',
  sm: 'small',
  md: 'small',
  lg: 'body',
};

const StyledBadge = styled(MantineBadge)<{ color: ColorVariant; size: SupportedMantineSize }>(
  ({ theme, color, size }) => css`
    text-transform: none;
    background-color: ${color};
    color: ${theme.utils.contrastingColor(color)};

    .mantine-Badge-label {
      font-size: ${theme.fonts.size[mapFontSize[size]]};
      overflow: visible;
    }
  `,
);

type Props = React.PropsWithChildren<{
  bsStyle?: ColorVariant;
  className?: string;
  'data-testid'?: string;
  onClick?: () => void;
  title?: string;
  bsSize?: BsSize;
}>;

const Badge = (
  {
    bsStyle = 'default',
    className = undefined,
    children = undefined,
    'data-testid': dataTestid,
    onClick = undefined,
    title = undefined,
    bsSize = 'md',
  }: Props,
  ref: React.ForwardedRef<HTMLDivElement>,
) => {
  const theme = useTheme();
  const color = mapStyle(bsStyle, theme);
  const size = sizeForMantine(bsSize);

  return (
    <StyledBadge
      color={color}
      className={className}
      title={title}
      data-testid={dataTestid}
      ref={ref}
      variant="filled"
      onClick={onClick}
      size={size}>
      {children}
    </StyledBadge>
  );
};

export default React.forwardRef(Badge);
