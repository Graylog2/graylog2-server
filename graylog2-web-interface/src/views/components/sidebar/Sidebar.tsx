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
import { useState, useContext } from 'react';
import chroma from 'chroma-js';
import styled, { css } from 'styled-components';

import type QueryResult from 'views/logic/QueryResult';
import type { SearchPreferencesLayout } from 'views/components/contexts/SearchPagePreferencesContext';
import SearchPagePreferencesContext from 'views/components/contexts/SearchPagePreferencesContext';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import zIndices from 'theme/z-indices';
import type { LayoutSidebarTitle } from 'views/components/contexts/SearchPageLayoutContext';

import SidebarNavigation from './SidebarNavigation';
import ContentColumn from './ContentColumn';
import type { SidebarSection } from './sidebarSections';
import sidebarSections from './sidebarSections';
import type { SidebarAction } from './sidebarActions';
import sidebarActions from './sidebarActions';

type Props = {
  actions?: Array<SidebarAction>;
  children?: React.ReactElement;
  enableSidebarPinning?: boolean;
  forceSideBarPinned?: boolean;
  results?: QueryResult;
  searchPreferencesLayout?: SearchPreferencesLayout;
  sections?: Array<SidebarSection>;
  title: LayoutSidebarTitle;
  contentColumnWidth?: number;
};

const Container = styled.div`
  display: flex;
  height: 100%;
  width: min-content;
`;

const ContentOverlay = styled.div(
  ({ theme }) => css`
    position: fixed;
    inset: 0 0 0 50px;
    background: ${chroma(theme.colors.brand.tertiary).alpha(0.25).css()};
    z-index: ${zIndices.sidebarOverlay};
  `,
);

const _toggleSidebar = (
  initialSectionKey: string,
  activeSectionKey: string | undefined | null,
  setActiveSectionKey,
) => {
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

const Sidebar = ({
  searchPreferencesLayout = undefined,
  results = undefined,
  children = undefined,
  title,
  sections = sidebarSections,
  actions = sidebarActions,
  forceSideBarPinned = false,
  enableSidebarPinning = true,
  contentColumnWidth = 275,
}: Props) => {
  const sendTelemetry = useSendTelemetry();
  const sidebarIsPinned = searchPreferencesLayout?.config.sidebar.isPinned || forceSideBarPinned;
  const initialSectionKey = sections[0].key;
  const [activeSectionKey, setActiveSectionKey] = useState<string | undefined>(
    sidebarIsPinned ? initialSectionKey : null,
  );
  const activeSection = sections.find((section) => section.key === activeSectionKey);

  const toggleSidebar = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_SIDEBAR_TOGGLE, {
      app_action_value: 'search_sidebar',
      initialSectionKey,
      activeSectionKey,
    });

    _toggleSidebar(initialSectionKey, activeSectionKey, setActiveSectionKey);
  };

  const selectSidebarSection = (sectionKey: string) =>
    _selectSidebarSection(sectionKey, activeSectionKey, setActiveSectionKey, toggleSidebar);
  const SectionContent = activeSection?.content;

  return (
    <Container>
      <SidebarNavigation
        activeSection={activeSection}
        selectSidebarSection={selectSidebarSection}
        sections={sections}
        sidebarIsPinned={sidebarIsPinned}
        actions={actions}
      />
      {activeSection && !!SectionContent && (
        <ContentColumn
          closeSidebar={toggleSidebar}
          title={title}
          enableSidebarPinning={enableSidebarPinning}
          searchPreferencesLayout={searchPreferencesLayout}
          sectionTitle={activeSection.title}
          forceSideBarPinned={forceSideBarPinned}
          width={contentColumnWidth}>
          <SectionContent
            results={results}
            sidebarChildren={children}
            sidebarIsPinned={sidebarIsPinned}
            toggleSidebar={toggleSidebar}
          />
        </ContentColumn>
      )}
      {activeSection && !sidebarIsPinned && <ContentOverlay onClick={toggleSidebar} />}
    </Container>
  );
};

const SidebarWithContext = ({ children = undefined, ...props }: React.ComponentProps<typeof Sidebar>) => {
  const searchPreferencesLayout = useContext(SearchPagePreferencesContext);

  return (
    <Sidebar {...props} searchPreferencesLayout={searchPreferencesLayout}>
      {children}
    </Sidebar>
  );
};

export default SidebarWithContext;
