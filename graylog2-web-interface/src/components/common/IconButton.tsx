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
import { forwardRef } from 'react';

import Icon from 'components/common/Icon';
import type { IconName, RotateProp, IconType } from 'components/common/Icon';
import { Button } from 'components/bootstrap';
import type { StyleProps } from 'components/bootstrap/Button';
import Tooltip from 'components/common/Tooltip';
import type { BsSize } from 'components/bootstrap/types';

const Wrapper = styled(Button)<{ disabled: boolean }>(
  ({ theme, disabled, bsStyle }) => css`
    ${bsStyle === 'transparent' &&
    css`
      padding: 0 3px;
      color: ${disabled ? theme.colors.gray[90] : theme.colors.gray[60]};
    `}
  `,
);

type Props = {
  focusable?: boolean;
  title: string;
  /** Stable accessible name, independent of the tooltip content. Defaults to `title`. */
  ariaLabel?: string;
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void;
  className?: string;
  name: IconName;
  iconType?: IconType;
  disabled?: boolean;
  rotation?: RotateProp;
  'data-testid'?: string;
  size?: BsSize;
  bsStyle?: StyleProps;
  iconSize?: 'lg' | 'inherit';
  showTitle?: boolean;
  href?: string;
  target?: '_blank';
  rel?: 'noopener noreferrer';
  allowClickWhenDisabled?: boolean;
};

const handleClick = (
  onClick: (e: React.MouseEvent<HTMLButtonElement>) => void | undefined,
  disabled: boolean,
  e: React.MouseEvent<HTMLButtonElement>,
) => {
  if (disabled) {
    return;
  }

  if (typeof onClick === 'function') {
    onClick(e);
  }
};

const StyledIcon = styled(Icon)`
  line-height: inherit;
`;

const IconButton = (
  {
    title,
    ariaLabel = undefined,
    onClick = undefined,
    focusable = true,
    className = undefined,
    disabled = false,
    iconType = undefined,
    bsStyle = 'transparent',
    'data-testid': dataTestId = undefined,
    size = 'xs',
    showTitle = false,
    href = undefined,
    target = undefined,
    rel = undefined,
    allowClickWhenDisabled = false,
    ...rest
  }: Props,
  ref: React.ForwardedRef<HTMLButtonElement>,
) => {
  const button = (
    <Wrapper
      ref={ref}
      tabIndex={focusable ? 0 : -1}
      data-testid={dataTestId}
      aria-label={ariaLabel ?? title}
      aria-disabled={disabled}
      onClick={(e) => handleClick(onClick, disabled, e)}
      className={className}
      type="button"
      bsSize={size}
      bsStyle={bsStyle}
      href={href}
      target={target}
      rel={rel}
      allowClickWhenDisabled={allowClickWhenDisabled}
      disabled={disabled}>
      <StyledIcon type={iconType} size={bsStyle === 'transparent' ? 'lg' : undefined} {...rest} />
      {showTitle && ` ${title}`}
    </Wrapper>
  );

  if (showTitle && !disabled) {
    return button;
  }

  return (
    <Tooltip label={title} position="top" openDelay={750}>
      {button}
    </Tooltip>
  );
};

export default forwardRef(IconButton);
