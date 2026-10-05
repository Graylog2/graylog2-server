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

import { Card } from 'components/common';

const StyledCard = styled(Card)`
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  box-shadow: none;
`;

const StyledIconContainer = styled.span`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;

  /* BrandIcon has its own 20x20 container — scale it to match */
  > div {
    width: 24px;
    height: 24px;

    svg {
      width: 24px;
      height: 24px;
    }
  }
`;

type Props = React.PropsWithChildren<{
  title?: string;
}>;

const IconCard = ({ title = undefined, children = undefined }: Props) => (
  <StyledCard padding="sm">
    <StyledIconContainer title={title}>{children}</StyledIconContainer>
  </StyledCard>
);

export default IconCard;
