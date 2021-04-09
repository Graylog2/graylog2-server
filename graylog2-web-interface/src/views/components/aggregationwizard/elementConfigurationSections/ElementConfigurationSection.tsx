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

const SectionContainer = styled.div(({ theme }) => css`
  display: flex;
  background-color: ${theme.colors.variant.lightest.default};
  margin-bottom: 5px;
  border-radius: 3px;
  border: 1px solid ${theme.colors.variant.lighter.default};
  padding: 6px 5px 3px 7px;

  :last-of-type {
    margin-bottom: 0;
  }
`);

const SectionActions = styled.div`
  display: flex;
  flex-direction: column;
  min-width: 25px;
  margin-left: 5px;
`;

const SectionConfiguration = styled.div`
  flex: 1;
`;

type Props = {
  children: React.ReactNode,
  onRemove?: () => void,
};

const ElementConfigurationSection = ({ children, onRemove }: Props) => (
  <SectionContainer>
    <SectionConfiguration>
      {children}
    </SectionConfiguration>
    <SectionActions>
      {onRemove && <IconButton onClick={onRemove} name="trash" title="Remove" />}
    </SectionActions>
  </SectionContainer>
);

ElementConfigurationSection.defaultProps = {
  onRemove: undefined,
};

export default ElementConfigurationSection;
