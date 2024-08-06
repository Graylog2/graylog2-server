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

const FlexWrapper = styled.div(({ theme }) => css`
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
  disableCollapseButton?: boolean,
}>

/**
 * Simple section component. Currently only a "filled" version exists.
 */
const Section = ({ title, actions, headerLeftSection, collapsible, defaultCollapse, disableCollapseButton, children }: Props) => {
  const [opened, { toggle }] = useDisclosure(defaultCollapse);

  return (
    <Container>
      <Header>
        <FlexWrapper>
          <h2>{title}</h2>
          {headerLeftSection && <FlexWrapper>{headerLeftSection}</FlexWrapper>}
        </FlexWrapper>
        <FlexWrapper>
          {actions && <div>{actions}</div>}
          {collapsible && (
            <Button bsSize="sm"
                    bsStyle={opened ? 'primary' : 'default'}
                    onClick={toggle}
                    data-testid="collapseButton"
                    disabled={disableCollapseButton}>
              <Icon size="sm" name={opened ? 'keyboard_arrow_up' : 'keyboard_arrow_down'} />
            </Button>
          )}
        </FlexWrapper>
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
  disableCollapseButton: false,
};

export default Section;
