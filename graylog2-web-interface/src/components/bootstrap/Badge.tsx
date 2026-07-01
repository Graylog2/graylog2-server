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

    /* Let the badge shrink below its content width — as a flex/grid item (min-width: 0) and
       capped to its container (max-width: 100%) instead of Mantine's default width: fit-content.
       Without this the badge always stays as wide as its text, so the label below never gets a
       constrained width to truncate against. */
    min-width: 0;
    max-width: 100%;

    .mantine-Badge-label {
      font-size: ${theme.fonts.size[mapFontSize[size]]};
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  `,
);

type Props = React.PropsWithChildren<{
  'aria-label'?: string;
  bsSize?: BsSize;
  bsStyle?: ColorVariant;
  className?: string;
  'data-testid'?: string;
  onClick?: (e: React.MouseEvent) => void;
  onMouseEnter?: React.MouseEventHandler<HTMLElement>;
  onMouseLeave?: React.MouseEventHandler<HTMLElement>;
  role?: string;
  style?: React.CSSProperties;
  title?: string;
}>;

const Badge = (
  {
    'aria-label': ariaLabel = undefined,
    bsStyle = 'default',
    className = undefined,
    children = undefined,
    'data-testid': dataTestid,
    onClick = undefined,
    onMouseEnter = undefined,
    onMouseLeave = undefined,
    role = undefined,
    style = undefined,
    title = undefined,
    bsSize = 'md',
  }: Props,
  ref: React.ForwardedRef<HTMLElement>,
) => {
  const theme = useTheme();
  const color = mapStyle(bsStyle, theme);
  const size = sizeForMantine(bsSize);

  const sharedProps = {
    'aria-label': ariaLabel,
    color,
    className,
    title,
    'data-testid': dataTestid,
    role,
    style,
    variant: 'filled' as const,
    onMouseEnter,
    onMouseLeave,
    size,
  };

  if (onClick) {
    return (
      <StyledBadge
        {...sharedProps}
        style={{ cursor: 'pointer', ...style }}
        component="button"
        ref={ref as React.Ref<HTMLButtonElement>}
        onClick={onClick}>
        {children}
      </StyledBadge>
    );
  }

  return (
    <StyledBadge {...sharedProps} component="span" ref={ref as React.Ref<HTMLSpanElement>}>
      {children}
    </StyledBadge>
  );
};

export default React.forwardRef(Badge);
