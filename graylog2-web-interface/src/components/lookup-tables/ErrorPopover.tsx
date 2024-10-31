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
import styled from 'styled-components';

import { OverlayTrigger, Icon } from 'components/common';

type Props = {
  errorText: string,
  title: string,
  placement: 'bottom' | 'top' | 'right' | 'left',
};

const StyledSpan = styled.span`
  margin-right: 5px;
`;

const ErrorPopover = ({ errorText, title = 'Error', placement = 'bottom' }: Props) => (
  <OverlayTrigger trigger={['hover', 'focus']} placement={placement} overlay={errorText} title={title} width={400}>
    <StyledSpan>
      <Icon name="warning" className="text-danger" />
    </StyledSpan>
  </OverlayTrigger>
);

export default ErrorPopover;
