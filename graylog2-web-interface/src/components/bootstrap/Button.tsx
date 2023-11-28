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
import styled from 'styled-components';

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

const styleProps = (style: StyleProps) => {
  switch (style) {
    case 'default': return { color: 'gray' };
    case 'link': return { variant: 'subtle' };
    default: return { color: style };
  }
};

const StyledButton = styled(MantineButton)`
  font-weight: 400;
`;

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
  role?: string,
  tabIndex?: number,
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
        height: '34px',
        padding: '6px 12px',
      };
  }
};

const disabledStyles = (style: ColorVariant, theme: MantineTheme) => {
  const colors = theme.other.colors.disabled[style];

  return {
    ':disabled': {
      color: colors.color,
      backgroundColor: colors.background,
    },
  };
};

const generateStyles = (theme: MantineTheme, bsStyle: StyleProps, bsSize: BsSize, disabled: boolean) => {
  const sizeStyles = stylesForSize(bsSize);
  const disableStyles = (disabled && bsStyle !== 'link' ? disabledStyles(bsStyle, theme) : {});

  return {
    root: {
      ...sizeStyles,
      ':disabled': disableStyles,
    },
    label: {
      gap: '0.25em',
    },
  };
};

const Button = React.forwardRef<HTMLButtonElement, Props>(
  ({
    'aria-label': ariaLabel, bsStyle, bsSize, className, 'data-testid': dataTestId, id, onClick, disabled, href,
    title, form, type, role, name, tabIndex, children,
  }, ref) => {
    const theme = useMantineTheme();
    const styles = useMemo(() => generateStyles(theme, bsStyle, bsSize, disabled), [bsSize, bsStyle, disabled, theme]);
    const button = (
      <StyledButton ref={ref}
                    id={id}
                    aria-label={ariaLabel}
                    className={className}
                    {...styleProps(bsStyle)}
                    data-testid={dataTestId}
                    disabled={disabled}
                    form={form}
                    name={name}
                    onClick={onClick as (e: React.MouseEvent<HTMLButtonElement>) => void}
                    role={role}
                    size={sizeForMantine(bsSize)}
                    styles={styles}
                    tabIndex={tabIndex}
                    title={title}
                    type={type}>{children}
      </StyledButton>
    );

    return href ? <a href={href}>{button}</a> : button;
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
  role: undefined,
  tabIndex: undefined,
  title: undefined,
  type: undefined,
};

export default Button;
