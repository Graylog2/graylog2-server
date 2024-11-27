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

import Icon from 'components/common/Icon';
import type { IconName, RotateProp, IconType, SizeProp } from 'components/common/Icon';

const Wrapper = styled.button<{ disabled: boolean }>(({ theme, disabled }) => css`
  display: inline-flex;
  justify-content: center;
  align-items: center;
  height: 25px;
  width: 25px;
  border: 0;
  background-color: transparent;
  cursor: pointer;
  color: ${disabled ? theme.colors.gray[90] : theme.colors.gray[60]};
  font-size: ${theme.fonts.size.large};

  &:hover {
    background-color: ${theme.colors.gray[80]};
  }

  &:active {
    background-color: ${theme.colors.gray[70]};
  }
`);

type Props = {
  focusable?: boolean,
  title: string,
  onClick?: () => void,
  className?: string,
  name: IconName
  iconType?: IconType,
  disabled?: boolean,
  rotation?: RotateProp,
  'data-testid'?: string,
  size?: SizeProp,
};

const handleClick = (onClick: () => void | undefined) => {
  if (typeof onClick === 'function') {
    onClick();
  }
};

const IconButton = React.forwardRef<HTMLButtonElement, Props>(({
  title,
  onClick,
  focusable = true,
  className,
  disabled = false,
  iconType,
  'data-testid': dataTestId,
  ...rest
}: Props, ref) => (
  <Wrapper ref={ref}
           tabIndex={focusable ? 0 : -1}
           data-testid={dataTestId}
           title={title}
           aria-label={title}
           onClick={() => handleClick(onClick)}
           className={className}
           type="button"
           disabled={disabled}>
    <Icon type={iconType} {...rest} />
  </Wrapper>
));

export default IconButton;
