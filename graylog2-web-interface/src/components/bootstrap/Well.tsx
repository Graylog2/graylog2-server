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

import Alert from './Alert';

type Padding = 'small' | 'large';

const StyledAlert = styled(Alert)<{ $padding: Padding }>(({ theme, $padding }) => css`
  background-color: ${theme.colors.variant.lightest.default};
  border-color: ${theme.colors.variant.light.default};
  margin-top: 0;
  padding: ${$padding === 'small' ? theme.spacings.sm : theme.spacings.md};

  .mantine-Alert-message {
    color: ${theme.colors.variant.darker.default};
  }
`);

type Props = Pick<React.ComponentProps<typeof StyledAlert>, 'children' | 'className' | 'style'> & {
  bsSize?: Padding,
};
// eslint-disable-next-line react/require-default-props
const Well = (props: Props) => <StyledAlert noIcon $padding={props.bsSize} {...props} />;

/** @component */
export default Well;
