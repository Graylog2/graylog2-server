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
import { useMemo } from 'react';
import type { ColorVariant } from '@graylog/sawmill';
import type { MantineTheme } from '@graylog/sawmill/mantine';
import { Button as MantineButton, useMantineTheme } from '@mantine/core';

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

export type StyleProps = ColorVariant | 'link';

const mapStyle = (style: StyleProps) => (style === 'default' ? 'gray' : style);

const styleProps = (style: StyleProps) => {
  switch (style) {
    case 'default': return { color: 'gray' };
    case 'link': return { variant: 'subtle' };
    default: return { color: style };
  }
};

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
  tabIndex?: number,
  target?: '_blank',
  title?: string,
  type?: 'button' | 'reset' | 'submit',
}>;

const stylesForSize = (size: BsSize) => {
  switch (size) {
    case 'xs':
    case 'xsmall':
      return {
        height: '21.4141px',
        padding: '1px 5px',
      };
    case 'sm':
    case 'small':
      return {
        height: '29.4141px',
        padding: '5px 10px',
      };
    case 'lg':
    case 'large':
      return {
        height: '43.1641px',
        padding: '10px 16px',
      };
    case 'medium':
    default:
      return {
        height: '33.84px',
        padding: '6px 12px',
      };
  }
};

type Other = MantineTheme['other'];

const disabledStyles = (style: ColorVariant, other: Other) => {
  const colors = other.colors.disabled[style];

  return {
    ':disabled': {
      pointerEvents: 'all',
      color: colors.color,
      backgroundColor: colors.background,
      opacity: '0.65',
    },
  };
};

const generateStyles = (other: Other, bsStyle: StyleProps, bsSize: BsSize, disabled: boolean) => {
  const sizeStyles = stylesForSize(bsSize);
  const disableStyles = (disabled && bsStyle !== 'link' ? disabledStyles(bsStyle, other) : {});

  return {
    root: {
      ...sizeStyles,
      color: other.colors.contrast[bsStyle],
      fontWeight: 400,
      ':disabled': disableStyles,
      ':hover': {
        color: other.colors.contrast[bsStyle],
        textDecoration: 'none',
      },
      ':focus': {
        color: other.colors.contrast[bsStyle],
        textDecoration: 'none',
      },
    },
    label: {
      gap: '0.25em',
      overflow: 'visible',
    },
  };
};

const Button = React.forwardRef<HTMLButtonElement, Props>(
  ({
    'aria-label': ariaLabel, bsStyle, bsSize, className, 'data-testid': dataTestId, id, onClick, disabled, href,
    title, form, target, type, rel, role, name, tabIndex, children,
  }, ref) => {
    const theme = useMantineTheme();
    const style = mapStyle(bsStyle);
    const styles = useMemo(() => generateStyles(theme.other, style, bsSize, disabled), [bsSize, disabled, style, theme.other]);

    const sharedProps = {
      id,
      'aria-label': ariaLabel,
      className,
      ...styleProps(style),
      'data-testid': dataTestId,
      disabled,
      role,
      size: sizeForMantine(bsSize),
      styles,
      tabIndex,
      title,
      type,
    } as const;

    if (href) {
      return (
        <MantineButton component="a"
                       href={href}
                       target={target}
                       rel={rel}
                       onClick={onClick as (e: React.MouseEvent<HTMLAnchorElement>) => void}
                       {...sharedProps}>
          {children}
        </MantineButton>
      );
    }

    return (
      <MantineButton ref={ref}
                     form={form}
                     onClick={onClick as (e: React.MouseEvent<HTMLButtonElement>) => void}
                     name={name}
                     {...sharedProps}>
        {children}
      </MantineButton>
    );
  });

Button.defaultProps = {
  active: undefined,
  'aria-label': undefined,
  bsStyle: 'default',
  bsSize: undefined,
  className: undefined,
  'data-testid': undefined,
  disabled: false,
  form: undefined,
  href: undefined,
  id: undefined,
  name: undefined,
  onClick: undefined,
  rel: undefined,
  role: undefined,
  tabIndex: undefined,
  target: undefined,
  title: undefined,
  type: undefined,
};

export default Button;
