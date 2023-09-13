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
import { Alert as MantineAlert, useMantineTheme } from '@mantine/core';

import Icon from 'components/common/Icon';

type Props = {
  bsStyle: ColorVariant,
  children: React.ReactNode,
  className?: string,
  onDismiss?: () => void,
  style?: CSSProperties,
  title?: React.ReactNode,
}

const StyledAlert = styled(MantineAlert)(({ theme }) => css`
  margin: ${theme.mantine.spacing.md} 0;
`);

const iconNameForType = (bsStyle: ColorVariant) => {
  switch (bsStyle) {
    case 'warning':
    case 'danger':
      return 'exclamation-triangle';
    case 'success':
      return 'circle-check';
    default:
      return 'info-circle';
  }
};

const Alert = ({ children, bsStyle, title, style, className, onDismiss }: Props) => {
  const theme = useMantineTheme();
  const alertStyles = () => ({
    message: {
      fontSize: theme.fontSizes.md,
    },
    title: {
      fontSize: theme.fontSizes.md,
    },
  });
  const iconName = iconNameForType(bsStyle);

  return (
    <StyledAlert className={className}
                 color={bsStyle}
                 style={style}
                 styles={alertStyles}
                 onClose={onDismiss}
                 title={title}
                 icon={<Icon name={iconName} />}
                 withCloseButton={typeof onDismiss === 'function'}>
      {children}
    </StyledAlert>
  );
};

Alert.defaultProps = {
  className: undefined,
  onDismiss: undefined,
  style: undefined,
  title: undefined,
};

export default Alert;
