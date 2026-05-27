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
import React from 'react';
import { Tabs as MantineTabs } from '@mantine/core';
import styled, { css } from 'styled-components';

import Tab from './Tab';
import type { TabProps } from './Tab';

type TabsProps = {
  children: React.ReactNode;
  defaultActiveKey?: string | number;
  activeKey?: string | number | null;
  onSelect?: (eventKey: string | null) => void;
  id?: string;
  animation?: boolean;
  className?: string;
  onClick?: React.MouseEventHandler;
};

const StyledTabs = styled(MantineTabs)(
  ({ theme }) => css`
    .mantine-Tabs-list {
      border-bottom-color: ${theme.colors.variant.default};
      flex-wrap: wrap;
    }

    .mantine-Tabs-tab {
      color: ${theme.colors.text.primary};
      border-color: ${theme.colors.variant.lighter.default} ${theme.colors.variant.lighter.default}
        ${theme.colors.variant.default};
      transition: background-color 150ms ease-in-out;

      &:hover:not([data-active]):not([data-disabled]) {
        background-color: ${theme.colors.variant.lightest.default};
        color: ${theme.colors.text.primary};
      }

      &[data-active] {
        color: ${theme.colors.variant.darkest.default};
        background-color: ${theme.colors.global.contentBackground};
        border-color: ${theme.colors.variant.default};
        border-bottom-color: ${theme.colors.global.contentBackground};
      }

      &[data-disabled] {
        color: ${theme.colors.gray[60]};
        background-color: ${theme.colors.gray[100]};
        border-color: ${theme.colors.gray[100]} ${theme.colors.gray[100]}
          ${theme.colors.variant.default};
        cursor: not-allowed;
        opacity: 1;
      }
    }

    .mantine-Tabs-panel {
      background-color: ${theme.colors.global.contentBackground};
      border: 1px solid ${theme.colors.variant.default};
      border-top: 0;
      border-radius: 0 0 4px 4px;
      padding: 9px;
    }
  `,
);

const Tabs = ({
  children,
  defaultActiveKey,
  activeKey,
  onSelect,
  id: _id,
  animation: _animation,
  className,
  ...rest
}: TabsProps) => {
  const tabChildren = React.Children.toArray(children).filter(
    (child): child is React.ReactElement<TabProps> =>
      React.isValidElement(child) && child.type === Tab,
  );

  const nonTabChildren = React.Children.toArray(children).filter(
    (child) => !React.isValidElement(child) || child.type !== Tab,
  );

  const mantineValue =
    activeKey !== undefined ? (activeKey !== null ? String(activeKey) : null) : undefined;

  return (
    <StyledTabs
      defaultValue={defaultActiveKey !== undefined ? String(defaultActiveKey) : undefined}
      value={mantineValue}
      onChange={onSelect}
      className={className}
      variant="outline"
      {...rest}>
      <MantineTabs.List>
        {tabChildren.map((tab) => (
          <MantineTabs.Tab
            key={tab.props.eventKey}
            value={String(tab.props.eventKey)}
            disabled={tab.props.disabled}
            className={tab.props.tabClassName}>
            {tab.props.title}
          </MantineTabs.Tab>
        ))}
      </MantineTabs.List>
      {tabChildren.map((tab) => (
        <MantineTabs.Panel key={tab.props.eventKey} value={String(tab.props.eventKey)}>
          {tab.props.children}
        </MantineTabs.Panel>
      ))}
      {nonTabChildren}
    </StyledTabs>
  );
};

/** @component */
export default Tabs;
