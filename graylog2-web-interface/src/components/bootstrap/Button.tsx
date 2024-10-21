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
import { Button as MantineButton } from '@mantine/core';
import type { DefaultTheme } from 'styled-components';
import styled, { useTheme, css } from 'styled-components';
import { Link } from 'react-router-dom';

import type { BsSize } from 'components/bootstrap/types';

const sizeForMantine = (size: BsSize) => {
  switch (size) {
    case 'xs':
    case 'xsmall': return 'xs';
    case 'sm':
    case 'small': return 'sm';
    case 'lg':
    case 'large': return 'lg';
    default: return 'md';
  }
};

export type StyleProps = ColorVariant | 'link' | 'transparent';

const isLinkStyle = (style: StyleProps) => style === 'link';
const isTransparentStyle = (style: StyleProps) => style === 'transparent';
const mapStyle = (style: StyleProps) => (style === 'default' ? 'gray' : style);

const stylesProps = (style: StyleProps) => {
  switch (style) {
    case 'link': return { variant: 'subtle' };
    default: return {};
  }
};

const stylesForSize = (size: BsSize, bsStyle: StyleProps) => {
  if (isLinkStyle(bsStyle)) {
    return css`
      padding: 0;
      height: auto;
    `;
  }

  switch (size) {
    case 'xs':
    case 'xsmall':
      return css`
        height: 21.4141px;
        padding: 1px 5px;
      `;
    case 'sm':
    case 'small':
      return css`
        height: 29.4141px;
        padding: 5px 10px;
      `;
    case 'lg':
    case 'large':
      return css`
        height: 43.1641px;
        padding: 10px 16px;
      `;
    case 'medium':
    default:
      return css`
        height: 33.84px;
        padding: 6px 12px;
      `;
  }
};

const disabledStyles = (themeColors: DefaultTheme['colors'], style: StyleProps) => {
  const isSpecialStyle = isLinkStyle(style) || isTransparentStyle(style);

  const colors = isSpecialStyle
    ? { color: themeColors.global.textDefault, background: 'transparent' }
    : themeColors.disabled[style];

  return css`
    &:disabled,
    &[data-disabled] {
      pointer-events: all;
      color: ${colors.color};
      background-color: ${colors.background};
      opacity: 0.45;

      &:hover {
        color: ${colors.color};
        text-decoration: none;
      }
    }
  `;
};

const activeStyles = (themeColors: DefaultTheme['colors'], bsStyle: StyleProps) => {
  switch (bsStyle) {
    case 'danger':
    case 'info':
    case 'success':
    case 'primary':
    case 'warning':
    case 'transparent':
      return css`
          color: ${themeColors.global.textDefault};

        &:hover {
          color: ${themeColors.global.textDefault};
        }

        &:focus {
          color: ${themeColors.global.textDefault};
        }
    `;
    default: return '';
  }
};

// Other link styles are defined in e.g. the size specific function
const linkStyles = css`
  vertical-align: baseline;

  &:hover {
    background: transparent;
    text-decoration: underline;
  }
`;

// Other transparent styles are defined in e.g. the size specific function
const transparentStyles = css`
  &:hover {
    background: transparent;
  }
`;

const textColor = (style: StyleProps, colors: DefaultTheme['colors']) => {
  switch (style) {
    case 'link':
      return colors.global.link;
    case 'transparent':
      return colors.global.textDefault;
    default:
      return colors.button[style].color;
  }
};

const StyledButton = styled(MantineButton)<{
  $bsStyle: StyleProps,
  $bsSize: BsSize,
  $active: boolean
}>(({
  theme,
  $bsStyle,
  $bsSize,
  $active,
}) => {
  const isLink = isLinkStyle($bsStyle);
  const isTransparent = isTransparentStyle($bsStyle);
  const color = textColor($bsStyle, theme.colors);

  return css`
    color: ${color};
    font-weight: 400;
    overflow: visible;

    ${disabledStyles(theme.colors, $bsStyle)}
    ${stylesForSize($bsSize, $bsStyle)}

    &:hover {
      color: ${isLink ? theme.colors.global.linkHover : color};
      text-decoration: none;
    }

    &:focus {
      color: ${color};
      text-decoration: none;
    }

    ${$active && activeStyles(theme.colors, $bsStyle)}
    ${isLink && linkStyles}
    ${isTransparent && transparentStyles}

    .mantine-Button-label {
      gap: 0.25em;
      overflow: visible;
    }

    .mantine-Button-loader {
      visibility: hidden;
    }
  `;
});

type Props = React.PropsWithChildren<{
  active?: boolean,
  'aria-label'?: string,
  bsStyle?: StyleProps,
  bsSize?: BsSize,
  className?: string,
  'data-testid'?: string,
  disabled?: boolean,
  form?: string,
  href?: string,
  id?: string,
  name?: string,
  onClick?: ((e: React.MouseEvent<HTMLButtonElement>) => void) | ((e: boolean) => void) | (() => void),
  rel?: 'noopener noreferrer',
  role?: string,
  style?: React.ComponentProps<typeof StyledButton>['style'],
  tabIndex?: number,
  target?: '_blank',
  title?: string,
  type?: 'button' | 'reset' | 'submit',
}>;

const Button = React.forwardRef<HTMLButtonElement, Props>(
  ({
    'aria-label': ariaLabel, bsStyle = 'default', bsSize, className, 'data-testid': dataTestId, id, onClick, disabled = false, href,
    title, form, target, type, rel, role, name, tabIndex, children, active,
  }, ref) => {
    const theme = useTheme();
    const style = mapStyle(bsStyle);
    const color = (isLinkStyle(style) || isTransparentStyle(style))
      ? 'transparent'
      : theme.colors.button[style].background;

    const sharedProps = {
      id,
      'aria-label': ariaLabel,
      className,
      ...stylesProps(style),
      $active: active,
      $bsStyle: style,
      $bsSize: bsSize,
      variant: active ? 'outline' : 'filled',
      color,
      'data-testid': dataTestId,
      disabled,
      role,
      size: sizeForMantine(bsSize),
      tabIndex,
      title,
      type,
    } as const;

    if (href) {
      return (
        <StyledButton component={Link}
                      to={href}
                      target={target}
                      rel={rel}
                      onClick={onClick as (e: React.MouseEvent<HTMLAnchorElement>) => void}
                      {...sharedProps}>
          {children}
        </StyledButton>
      );
    }

    return (
      <StyledButton ref={ref}
                    form={form}
                    onClick={onClick as (e: React.MouseEvent<HTMLButtonElement>) => void}
                    name={name}
                    {...sharedProps}>
        {children}
      </StyledButton>
    );
  });

export default Button;
