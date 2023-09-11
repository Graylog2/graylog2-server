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
import type { CSSProperties } from 'react';
import styled, { css, useTheme } from 'styled-components';
import { Alert as MantineAlert } from '@mantine/core';
import type { ColorVariant } from '@graylog/sawmill';

const StyledAlert = styled(MantineAlert)(({ theme }) => css`
  margin: ${theme.mantine.spacing.md} 0;
`);

type Props = {
  bsStyle: ColorVariant,
  children: React.ReactNode,
  className?: string,
  icon?: React.ReactNode,
  onClose?: () => void,
  style?: CSSProperties,
  title?: React.ReactNode,
};

const Alert = ({ children, bsStyle, onClose, icon, title, style, className }: Props) => {
  const theme = useTheme();
  const alertStyles = () => ({
    message: {
      fontSize: theme.mantine.fontSizes.md,
    },
    title: {
      fontSize: theme.mantine.fontSizes.md,
    },
  });

  return (
    <StyledAlert className={className}
                 icon={icon}
                 color={bsStyle}
                 onClose={onClose}
                 style={style}
                 styles={alertStyles}
                 title={title}
                 withCloseButton={typeof onClose === 'function'}>
      {children}
    </StyledAlert>
  );
};

Alert.defaultProps = {
  className: undefined,
  icon: undefined,
  onClose: undefined,
  style: undefined,
  title: undefined,
};

export default Alert;
