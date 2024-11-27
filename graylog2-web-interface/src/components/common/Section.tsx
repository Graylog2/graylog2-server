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

const Container = styled.div<{ $collapsible: boolean, $opened: boolean}>(({ $collapsible, $opened, theme }) => css`
  background-color: ${theme.colors.section.filled.background};
  border: 1px solid ${theme.colors.section.filled.border};
  border-radius: 10px;
  padding: ${($collapsible && !$opened) ? 0 : theme.spacings.md};
  margin-bottom: ${theme.spacings.xxs};
`);

const Header = styled.div<{ $collapsible: boolean, $opened: boolean}>(({ $collapsible, $opened, theme }) => css`
  display: flex;
  justify-content: space-between;
  gap: ${theme.spacings.xs};
  align-items: center;
  border-radius: 10px;
  padding: ${($collapsible && !$opened) ? theme.spacings.md : 0};
  flex-wrap: wrap;

    &:hover {
    background-color: ${($collapsible && !$opened) ? theme.colors.table.row.backgroundHover : 'initial'};
  }
`);

const FlexWrapper = styled.div(({ theme }) => css`
  display: flex;
  justify-content: flex-start;
  gap: ${theme.spacings.sm};
  align-items: center;
`);

type Props = React.PropsWithChildren<{
  title: string,
  header?: React.ReactNode,
  actions?: React.ReactNode,
  headerLeftSection?: React.ReactNode,
  collapsible?: boolean,
  onCollapse?: (opened?: boolean) => void,
  defaultClosed?: boolean,
  disableCollapseButton?: boolean,
}>

/**
 * Simple section component. Currently only a "filled" version exists.
 */
const Section = ({
  title,
  header = null,
  actions = null,
  headerLeftSection = null,
  collapsible = false,
  defaultClosed = false,
  onCollapse = () => {},
  disableCollapseButton = false,
  children = null,
}: Props) => {
  const [opened, { toggle }] = useDisclosure(!defaultClosed);

  const onToggle = () => {
    toggle();
    onCollapse(opened);
  };

  const onHeaderClick = () => (!disableCollapseButton && onToggle());

  return (
    <Container $opened={opened} $collapsible={collapsible}>
      <Header $opened={opened} $collapsible={collapsible} onClick={onHeaderClick}>
        <FlexWrapper>
          <h2>{header ?? title}</h2>
          {headerLeftSection && <FlexWrapper onClick={(e) => { e.stopPropagation(); }}>{headerLeftSection}</FlexWrapper>}
        </FlexWrapper>
        <FlexWrapper>
          {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events,jsx-a11y/no-static-element-interactions */}
          {actions && <div onClick={(e) => { e.stopPropagation(); }}>{actions}</div>}
          {collapsible && (
          <Button bsSize="sm"
                  bsStyle={opened ? 'primary' : 'default'}
                  onClick={toggle}
                  data-testid="collapseButton"
                  title={`Toggle ${title.toLowerCase()} section`}
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

export default Section;
