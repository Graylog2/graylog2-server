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
import styled, { css, useTheme } from 'styled-components';
import { Alert as MantineAlert } from '@mantine/core';

import type { ColorVariants } from '../../theme/types';

const StyledAlert = styled(MantineAlert)(({ theme }) => css`
  margin: ${theme.spacings.md} 0;
`);

type Props = {
  children: React.ReactNode,
  type: ColorVariants,
};

const Alert = ({ children, type }: Props) => {
  const theme = useTheme();

  const alertStyles = () => ({
    root: {
      borderColor: theme.colors.variant.lighter[type],
      backgroundColor: `${theme.colors.variant.lightest[type]} !important`,
    },
  });

  return (
    <StyledAlert styles={alertStyles} color={theme.colors.variant.lightest[type]}>
      {children}
    </StyledAlert>
  );
};

export default Alert;
