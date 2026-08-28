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
import type { BadgeVariant } from '@mantine/core';
import { Badge as MantineBadge } from '@mantine/core';
import styled, { css, useTheme } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

import type { BsSize } from 'components/bootstrap/types';
import Icon from 'components/common/Icon';
import type { IconName } from 'components/common/Icon';
import sizeForMantine from 'theme/utils/sizeForMantine';
import type { SupportedMantineSize } from 'theme/types';

export const BADGE_COLORS = [
  'primary',
  'danger',
  'success',
  'warning',
  'gray',
] as const satisfies readonly ColorVariant[];
export type BadgeColor = (typeof BADGE_COLORS)[number];

export const BADGE_VARIANTS = ['light', 'filled'] as const;
export type BadgeVariant = (typeof BADGE_VARIANTS)[number];

export const BADGE_SIZES = ['sm', 'md', 'lg'] as const;
export type BadgeSize = (typeof BADGE_SIZES)[number];

const mapStyle = (style: ColorVariant, theme: DefaultTheme) =>
  style === 'default' ? theme.colors.button.gray.background : theme.colors.variant[style];

const mapFontSize: Record<SupportedMantineSize, 'tiny' | 'small' | 'body'> = {
  xs: 'tiny',
  sm: 'small',
  md: 'small',
  lg: 'body',
};

const dotSize: Record<SupportedMantineSize, string> = {
  xs: '4px',
  sm: '4px',
  md: '6px',
  lg: '8px',
};

const iconSizeForBadge: Record<SupportedMantineSize, 'xs' | 'sm'> = {
  xs: 'xs',
  sm: 'xs',
  md: 'xs',
  lg: 'sm',
};

const StyledBadge = styled(MantineBadge)<{ $background: string; $color: string; size: SupportedMantineSize }>(
  ({ theme, $background, $color, size }) => css`
    text-transform: none;
    font-weight: 600;
    background-color: ${$background};
    color: ${$color};

    /* Let the badge shrink below its content width — as a flex/grid item (min-width: 0) and
       capped to its container (max-width: 100%) instead of Mantine's default width: fit-content.
       Without this the badge always stays as wide as its text, so the label below never gets a
       constrained width to truncate against. */
    min-width: 0;
    max-width: 100%;

    .mantine-Badge-label {
      display: flex;
      align-items: center;
      gap: ${theme.spacings.xxs};
      font-size: ${theme.fonts.size[mapFontSize[size]]};
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &.uppercase {
      text-transform: uppercase;
    }
  `,
);

const Dot = styled.span<{ $color: string; $size: SupportedMantineSize }>(
  ({ $color, $size }) => css`
    display: inline-block;
    flex-shrink: 0;
    width: ${dotSize[$size]};
    height: ${dotSize[$size]};
    border-radius: 50%;
    background-color: ${$color};
  `,
);

export type BadgeProps = React.PropsWithChildren<{
  'aria-label'?: string;
  /** @deprecated Legacy size alias — prefer `size`. Only used as a fallback when `size` is not set. */
  bsSize?: BsSize;
  /** @deprecated Legacy color variant — prefer `color`/`variant`. Only used as a fallback when `color` is not set. */
  bsStyle?: ColorVariant;
  className?: string;
  color?: BadgeColor;
  'data-testid'?: string;
  /** Only takes effect when `color` is set — a no-op on the legacy `bsStyle` path. */
  dot?: boolean;
  leftIcon?: IconName;
  onClick?: (e: React.MouseEvent) => void;
  onMouseEnter?: React.MouseEventHandler<HTMLElement>;
  onMouseLeave?: React.MouseEventHandler<HTMLElement>;
  rightIcon?: IconName;
  role?: string;
  size?: BadgeSize;
  style?: React.CSSProperties;
  title?: string;
  uppercase?: boolean;
  variant?: BadgeVariant;
}>;

const Badge = (
  {
    'aria-label': ariaLabel = undefined,
    bsSize = undefined,
    bsStyle = 'default',
    className = undefined,
    children = undefined,
    color = undefined,
    'data-testid': dataTestid,
    dot = false,
    leftIcon = undefined,
    onClick = undefined,
    onMouseEnter = undefined,
    onMouseLeave = undefined,
    rightIcon = undefined,
    role = undefined,
    size = undefined,
    style = undefined,
    title = undefined,
    uppercase = false,
    variant = 'light',
  }: BadgeProps,
  ref: React.ForwardedRef<HTMLElement>,
) => {
  const theme = useTheme();

  const resolvedSize: SupportedMantineSize = size ?? (bsSize ? sizeForMantine(bsSize) : 'md');
  const background = color ? theme.colors.badges[color][variant].background : mapStyle(bsStyle, theme);
  const textColor = color ? theme.colors.badges[color][variant].text : theme.utils.contrastingColor(background);
  const iconSize = iconSizeForBadge[resolvedSize];

  let leftSection: React.ReactNode;

  if (color && dot) {
    leftSection = <Dot $color={textColor} $size={resolvedSize} data-testid="badge-dot" />;
  } else if (leftIcon) {
    leftSection = <Icon name={leftIcon} size={iconSize} />;
  }

  const rightSection = rightIcon ? <Icon name={rightIcon} size={iconSize} /> : undefined;

  const sharedProps = {
    'aria-label': ariaLabel,
    $background: background,
    $color: textColor,
    className: uppercase ? `${className ?? ''} uppercase` : className,
    title,
    'data-testid': dataTestid,
    role,
    style,
    variant: 'filled' as const,
    leftSection,
    rightSection,
    onMouseEnter,
    onMouseLeave,
    size: resolvedSize,
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

const ForwardedBadge = React.forwardRef(Badge);
ForwardedBadge.displayName = 'Badge';

export default ForwardedBadge;
