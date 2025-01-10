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

import type { IconName } from 'components/common/Icon';

import Icon from './Icon';
import Delayed from './Delayed';

const Container = styled.span`
  display: inline-flex;
  align-items: center;
  vertical-align: baseline;
`;

const StyledIcon = styled(Icon)<{ $displayMargin: boolean }>(({ $displayMargin }) => css`
  ${$displayMargin ? 'margin-right: 6px;' : ''}
`);

type Props = {
  delay?: number,
  name?: IconName,
  text?: string,
  size?: React.ComponentProps<typeof StyledIcon>['size'],
  style?: React.ComponentProps<typeof StyledIcon>['style'],
};

/**
 * Simple spinner to use while waiting for something to load.
 */
const Spinner = ({ name = 'progress_activity', text = 'Loading...', delay = 200, ...rest }: Props) => (
  <Delayed delay={delay}>
    <Container>
      <StyledIcon {...rest} name={name} $displayMargin={!!text?.trim()} spin />{text}
    </Container>
  </Delayed>
);

export default Spinner;
