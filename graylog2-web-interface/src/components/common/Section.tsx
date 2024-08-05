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

const Container = styled.div(({ theme }) => css`
  background-color: ${theme.colors.section.filled.background};
  border: 1px solid ${theme.colors.section.filled.border};
  border-radius: 10px;
  padding: 15px;
  margin-bottom: ${theme.spacings.xxs};
`);

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  gap: 5px;
  align-items: center;
  margin-bottom: 10px;
  flex-wrap: wrap;
`;

const HeaderLeftWrapper = styled.div(({ theme }) => css`
  display: flex;
  justify-content: flex-start;
  gap: ${theme.spacings.sm};
  align-items: center;
`);

type Props = React.PropsWithChildren<{
  title: React.ReactNode,
  actions?: React.ReactNode,
  headerLeftSection?: React.ReactNode,
}>

/**
 * Simple section component. Currently only a "filled" version exists.
 */
const Section = ({ title, actions, headerLeftSection, children }: Props) => (
  <Container>
    <Header>
      <HeaderLeftWrapper>
        <h2>{title}</h2>
        {headerLeftSection && <div>{headerLeftSection}</div>}
      </HeaderLeftWrapper>
      {actions && <div>{actions}</div>}
    </Header>
    {children}
  </Container>
);

Section.defaultProps = {
  actions: undefined,
  headerLeftSection: undefined,
};

export default Section;
