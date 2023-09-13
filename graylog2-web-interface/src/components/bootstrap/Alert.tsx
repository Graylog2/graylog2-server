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
// eslint-disable-next-line no-restricted-imports
import type { ColorVariant } from '@graylog/sawmill';
import { Alert as MantineAlert } from '@mantine/core';

interface Props {
  bsStyle: ColorVariant,
  children: React.ReactNode,
  onDismiss?: () => void,
}

const StyledAlert = styled(MantineAlert)(({ theme }) => css`
  margin: ${theme.mantine.spacing.md} 0;
`);

const Alert = ({ bsStyle, ...rest }: Props) => <StyledAlert color={bsStyle} {...rest} />;

Alert.defaultProps = {
  onDismiss: undefined,
};

export default Alert;
