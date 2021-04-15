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

import { IconButton } from 'components/common';

const Container = styled.div(({ theme }) => css`
  display: flex;
  background-color: ${theme.colors.variant.lightest.default};
  border-radius: 3px;
  margin-bottom: 5px;
  padding: 6px 5px 3px 7px;
  border: 1px solid ${theme.colors.variant.lighter.default};

  :last-of-type {
    border-bottom: 0;
    margin-bottom: 0;
  }
`);

const ElementActions = styled.div`
  display: flex;
  flex-direction: column;
  min-width: 25px;
  margin-left: 3px;
`;

const ElementConfiguration = styled.div`
  flex: 1;
`;

type Props = {
  children: React.ReactNode,
  onRemove?: () => void,
};

const ElementConfigurationContainer = ({ children, onRemove }: Props) => (
  <Container>
    <ElementConfiguration>
      {children}
    </ElementConfiguration>
    <ElementActions>
      {onRemove && <IconButton onClick={onRemove} name="trash" title="Remove" />}
    </ElementActions>
  </Container>
);

ElementConfigurationContainer.defaultProps = {
  onRemove: undefined,
};

export default ElementConfigurationContainer;
