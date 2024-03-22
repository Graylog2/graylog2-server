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
import type { CSSProperties } from 'react';
import type { ColorVariant } from '@graylog/sawmill';
import { Alert as MantineAlert } from '@mantine/core';

import Icon from 'components/common/Icon';

type Props = {
  bsStyle?: ColorVariant,
  children: React.ReactNode,
  className?: string,
  displayIcon?: boolean,
  onDismiss?: () => void,
  style?: CSSProperties,
  title?: React.ReactNode,
}

const StyledAlert = styled(MantineAlert)<{ $bsStyle: ColorVariant }>(({ $bsStyle, theme }) => css`
  margin: ${theme.spacings.md} 0;
  border: 1px solid ${theme.colors.variant.lighter[$bsStyle]};

  .mantine-Alert-message {
    color: ${theme.colors.global.textDefault};
    font-size: ${theme.fonts.size.body};
  }

  .mantine-Alert-title {
    font-size: ${theme.fonts.size.body};
    color: ${theme.colors.global.textDefault};
  }

  .mantine-Alert-closeButton {
    color: ${theme.colors.global.textDefault};
  },
`);

const iconNameForType = (bsStyle: ColorVariant) => {
  switch (bsStyle) {
    case 'warning':
    case 'danger':
      return 'error';
    case 'success':
      return 'check_circle';
    default:
      return 'info';
  }
};

const Alert = ({ children, bsStyle, title, style, className, onDismiss, displayIcon }: Props) => {
  const displayCloseButton = typeof onDismiss === 'function';
  const iconName = iconNameForType(bsStyle);

  return (
    <StyledAlert $bsStyle={bsStyle}
                 className={className}
                 color={bsStyle}
                 style={style}
                 onClose={onDismiss}
                 title={title}
                 icon={displayIcon && <Icon name={iconName} />}
                 closeButtonLabel={displayCloseButton && 'Close alert'}
                 withCloseButton={displayCloseButton}>
      {children}
    </StyledAlert>
  );
};

Alert.defaultProps = {
  className: undefined,
  displayIcon: true,
  onDismiss: undefined,
  style: undefined,
  title: undefined,
  bsStyle: 'default',
};

export default Alert;
