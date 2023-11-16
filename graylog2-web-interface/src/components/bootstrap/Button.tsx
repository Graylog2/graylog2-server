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
import { Button as MantineButton } from '@mantine/core';
import type { ColorVariant } from '@graylog/sawmill';
import styled from 'styled-components';

type BootstrapSizes = 'xs' | 'sm' | 'lg' | 'xsmall' | 'small' | 'large';

const sizeForMantine = (size: BootstrapSizes) => {
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

const StyledButton = styled(MantineButton)`
  height: auto;
  padding: 6px 12px;
  font-weight: 400;
`;

type Props = {
  bsStyle?: ColorVariant,
  bsSize?: BootstrapSizes,
  onClick: () => void,
}
const Button = ({ bsStyle, bsSize, onClick, children }: React.PropsWithChildren<Props>) => (
  <StyledButton color={bsStyle} size={sizeForMantine(bsSize)} onClick={onClick}>{children}</StyledButton>
);

Button.defaultProps = {
  bsStyle: 'default',
  bsSize: undefined,
};

export default Button;
