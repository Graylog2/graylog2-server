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
import { useState } from 'react';
import chroma from 'chroma-js';
import styled, { css } from 'styled-components';

import type QueryResult from 'views/logic/QueryResult';
import type { SearchPreferencesLayout } from 'views/components/contexts/SearchPagePreferencesContext';
import SearchPagePreferencesContext from 'views/components/contexts/SearchPagePreferencesContext';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';

import SidebarNavigation from './SidebarNavigation';
import ContentColumn from './ContentColumn';
import type { SidebarSection } from './sidebarSections';
import sidebarSections from './sidebarSections';
import type { SidebarAction } from './sidebarActions';
import sidebarActions from './sidebarActions';

type Props = {
  children: React.ReactElement,
  results?: QueryResult
  searchPageLayout?: SearchPreferencesLayout,
  sections?: Array<SidebarSection>,
  actions?: Array<SidebarAction>,
  forceSideBarPinned?: boolean,
};

const Container = styled.div`
  display: flex;
  height: 100%;
  width: min-content;
`;

const ContentOverlay = styled.div(({ theme }) => css`
  position: fixed;
  inset: 0 0 0 50px;
  background: ${chroma(theme.colors.brand.tertiary).alpha(0.25).css()};
  z-index: 5;
`);

const _toggleSidebar = (initialSectionKey: string, activeSectionKey: string | undefined | null, setActiveSectionKey) => {
  if (activeSectionKey) {
    setActiveSectionKey(null);

    return;
  }

  setActiveSectionKey(initialSectionKey);
};

const _selectSidebarSection = (sectionKey, activeSectionKey, setActiveSectionKey, toggleSidebar) => {
  if (sectionKey === activeSectionKey) {
    toggleSidebar();

    return;
  }

  setActiveSectionKey(sectionKey);
};

const Sidebar = ({ searchPageLayout, results, children, sections = sidebarSections, actions = sidebarActions, forceSideBarPinned = false }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const location = useLocation();
  const queryId = useActiveQueryId();
  const sidebarIsPinned = searchPageLayout?.config.sidebar.isPinned || forceSideBarPinned;
  const initialSectionKey = sections[0].key;
  const [activeSectionKey, setActiveSectionKey] = useState<string | undefined>(searchPageLayout?.config.sidebar.isPinned ? initialSectionKey : null);
  const activeSection = sections.find((section) => section.key === activeSectionKey);

  const toggleSidebar = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_SIDEBAR_TOGGLE, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_action_value: 'search_sidebar',
      initialSectionKey,
      activeSectionKey,
    });

    _toggleSidebar(initialSectionKey, activeSectionKey, setActiveSectionKey);
  };

  const selectSidebarSection = (sectionKey: string) => _selectSidebarSection(sectionKey, activeSectionKey, setActiveSectionKey, toggleSidebar);
  const SectionContent = activeSection?.content;

  return (
    <Container>
      <SidebarNavigation activeSection={activeSection}
                         selectSidebarSection={selectSidebarSection}
                         sections={sections}
                         sidebarIsPinned={sidebarIsPinned}
                         actions={actions} />
      {activeSection && !!SectionContent && (
        <ContentColumn closeSidebar={toggleSidebar}
                       searchPageLayout={searchPageLayout}
                       sectionTitle={activeSection.title}
                       forceSideBarPinned={forceSideBarPinned}>
          <SectionContent results={results}
                          queryId={queryId}
                          sidebarChildren={children}
                          sidebarIsPinned={sidebarIsPinned}
                          toggleSidebar={toggleSidebar} />
        </ContentColumn>
      )}
      {(activeSection && !sidebarIsPinned) && (
        <ContentOverlay onClick={toggleSidebar} />
      )}
    </Container>
  );
};

const SidebarWithContext = ({ children, ...props }: React.ComponentProps<typeof Sidebar>) => (
  <SearchPagePreferencesContext.Consumer>
    {(searchPageLayout) => <Sidebar {...props} searchPageLayout={searchPageLayout}>{children}</Sidebar>}
  </SearchPagePreferencesContext.Consumer>
);

export default SidebarWithContext;
