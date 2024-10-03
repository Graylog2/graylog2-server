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

import type { SidebarAction } from 'views/components/sidebar/sidebarActions';
import type { IconName } from 'components/common/Icon';
import usePluginEntities from 'hooks/usePluginEntities';
import useLocation from 'routing/useLocation';

import NavItem from './NavItem';
import type { SidebarSection } from './sidebarSections';

export type SidebarPage = {
  key: string,
  title: string,
  icon: IconName,
  link: string,
};

const Container = styled.div<{ $isOpen: boolean, $sidebarIsPinned: boolean }>(({ $isOpen, $sidebarIsPinned, theme }) => css`
  background: ${theme.colors.global.navigationBackground};
  color: ${theme.utils.contrastingColor(theme.colors.global.navigationBackground, 'AA')};
  box-shadow: ${($sidebarIsPinned && $isOpen) ? 'none' : `3px 3px 3px ${theme.colors.global.navigationBoxShadow}`};
  width: 50px;
  height: 100%;
  position: relative;
  z-index: 1031;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    right: -6px;
    height: 6px;
    width: 6px;
    border-top-left-radius: 50%;
    background: transparent;
    box-shadow: -6px -6px 0 3px ${theme.colors.global.navigationBackground};
    z-index: 4; /* to render over Sidebar ContentColumn */
  }
`);

const SectionList = styled.div`
  > * {
    margin-bottom: 5px;

    &:last-child {
      margin-bottom: 0;
    }
  }
`;

const HorizontalRuleWrapper = styled.div`
  padding: 0 10px;

  hr {
    border-color: currentColor;
    margin: 5px 0 10px;
  }
`;

type Props = {
  activeSection: SidebarSection | undefined | null,
  sections: Array<SidebarSection>,
  actions: Array<SidebarAction>,
  selectSidebarSection: (sectionKey: string) => void,
  sidebarIsPinned: boolean,
};

const SidebarNavigation = ({ sections, activeSection, selectSidebarSection, sidebarIsPinned, actions }: Props) => {
  const activeSectionKey = activeSection?.key;
  const { pathname } = useLocation();
  const links = usePluginEntities('views.sidebar.links');

  return (
    <Container $sidebarIsPinned={sidebarIsPinned} $isOpen={!!activeSection}>
      {links?.length > 0 && (
        <>
          <SectionList>
            {links.map(({ icon, title, link, key }) => {
              const isSelected = link !== pathname;

              return (
                <NavItem isSelected={isSelected}
                         ariaLabel={`Open ${title}`}
                         icon={icon}
                         key={key}
                         linkTarget={link}
                         title={title} />
              );
            })}
          </SectionList>
          <HorizontalRuleWrapper><hr /></HorizontalRuleWrapper>
        </>
      )}
      <SectionList>
        {sections.map(({ key, icon, title }) => {
          const isSelected = activeSectionKey === key;

          return (
            <NavItem isSelected={isSelected}
                     ariaLabel={isSelected ? `Close ${title} section` : `Open ${title} section`}
                     icon={icon}
                     onClick={() => selectSidebarSection(key)}
                     key={key}
                     title={title}
                     sidebarIsPinned={sidebarIsPinned} />
          );
        })}
      </SectionList>
      <HorizontalRuleWrapper><hr /></HorizontalRuleWrapper>
      <SectionList>
        {actions.map(({ key, Component }) => (
          <Component key={key} sidebarIsPinned={sidebarIsPinned} />
        ))}
      </SectionList>
    </Container>
  );
};

export default SidebarNavigation;
