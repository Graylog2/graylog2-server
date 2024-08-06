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
import { useDisclosure } from '@mantine/hooks';
import { Collapse } from '@mantine/core';

import Icon from './Icon';

import { Button } from '../bootstrap';

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
const HeaderRightWrapper = styled.div(({ theme }) => css`
  display: flex;
  justify-content: flex-start;
  gap: ${theme.spacings.sm};
  align-items: center;
`);

type Props = React.PropsWithChildren<{
  title: React.ReactNode,
  actions?: React.ReactNode,
  headerLeftSection?: React.ReactNode,
  collapsible?: boolean,
  defaultCollapse?: boolean,
}>

/**
 * Simple section component. Currently only a "filled" version exists.
 */
const Section = ({ title, actions, headerLeftSection, collapsible, defaultCollapse, children }: Props) => {
  const [opened, { toggle }] = useDisclosure(defaultCollapse);

  return (
    <Container>
      <Header>
        <HeaderLeftWrapper>
          <h2>{title}</h2>
          {headerLeftSection && <div>{headerLeftSection}</div>}
        </HeaderLeftWrapper>
        <HeaderRightWrapper>
          {actions && <div>{actions}</div>}
          {collapsible && (
          <Button bsSize="xs" onClick={toggle} data-testid="collapseButton">
            <Icon name={opened ? 'keyboard_arrow_up' : 'keyboard_arrow_down'} />
          </Button>
          )}
        </HeaderRightWrapper>
      </Header>
      {!collapsible && children}
      {collapsible && (
      <Collapse in={opened}>
        {children}
      </Collapse>
      )}
    </Container>
  );
};

Section.defaultProps = {
  actions: undefined,
  headerLeftSection: undefined,
  collapsible: false,
  defaultCollapse: true,
};

export default Section;
