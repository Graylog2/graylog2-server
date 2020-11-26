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
import type { StyledComponent } from 'styled-components';

import type { ViewType } from 'views/logic/views/View';
import type { ThemeInterface } from 'theme';
import { SearchPageLayout } from 'views/components/contexts/SearchPageLayoutContext';
import { ViewMetaData as ViewMetadata } from 'views/stores/ViewMetadataStore';
import { IconButton } from 'components/common';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

type Props = {
  children: React.ReactNode,
  closeSidebar: () => void,
  searchPageLayout: SearchPageLayout | undefined | null,
  sectionTitle: string,
  viewIsNew: boolean,
  viewMetadata: ViewMetadata,
};

export const Container: StyledComponent<{ sidebarIsPinned: boolean }, ThemeInterface, HTMLDivElement> = styled.div(({ theme, sidebarIsPinned }) => css`
  position: ${sidebarIsPinned ? 'relative' : 'fixed'};
  width: 270px;
  height:  ${sidebarIsPinned ? '100%' : 'calc(100% - 50px)'}; // subtract the nav height
  top: ${sidebarIsPinned ? 0 : '50px'};
  left: ${sidebarIsPinned ? 0 : '50px'};

  background: ${theme.colors.global.contentBackground};
  border-right: ${sidebarIsPinned ? 'none' : `1px solid ${theme.colors.variant.light.default}`};
  box-shadow: ${sidebarIsPinned ? `3px 3px 3px ${theme.colors.global.navigationBoxShadow}` : 'none'};

  z-index: ${sidebarIsPinned ? 1030 : 3};

  ${sidebarIsPinned && css`
    ::before {
      content: '';
      position: absolute;
      top: 0;
      right: -6px;
      height: 6px;
      width: 6px;
      border-top-left-radius: 50%;
      background: transparent;
      box-shadow: -6px -6px 0px 3px ${theme.colors.global.contentBackground};
      z-index: 4; /* to render over Sidebar ContentColumn */
    }
  `}
`);

const ContentGrid = styled.div(({ theme }) => css`
  display: grid;
  display: -ms-grid;
  grid-template-columns: 1fr;
  grid-template-rows: auto minmax(1px, 1fr);
  -ms-grid-columns: 1fr;
  -ms-grid-rows: auto 1fr;
  height: 100%;
  overflow-y: auto;

  padding: 5px 15px 0 15px;

  color: ${theme.colors.global.textDefault};
`);

const Header = styled.div`
  grid-column: 1;
  -ms-grid-column: 1;
  grid-row: 1;
  -ms-grid-row: 1;
`;

const SearchTitle: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  height: 35px;
  display: grid;
  display: -ms-grid;
  grid-template-columns: 1fr auto;
  -ms-grid-columns: 1fr auto;

  > *:nth-child(1) {
    grid-column: 1;
    -ms-grid-column: 1;
  }

  > *:nth-child(2) {
    grid-column: 2;
    -ms-grid-column: 2;
  }
`;

const Title = styled.h1`
  color: inherit;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  line-height: 1.5;
`;

const OverlayToggle: StyledComponent<{ sidebarIsPinned: boolean }, ThemeInterface, HTMLDivElement> = styled.div(({ theme, sidebarIsPinned }) => css`
  > * {
    font-size: ${theme.fonts.size.large};
    color: ${sidebarIsPinned ? theme.colors.variant.info : theme.colors.gray[30]};
  }
`);

const HorizontalRule = styled.hr`
  margin: 5px 0 10px 0;
`;

const SectionTitle = styled.h2`
  margin-bottom: 10px;
`;

const CenterVertical = styled.div`
  display: inline-grid;
  display: -ms-inline-grid;
  align-content: center;
`;

const SectionContent = styled.div`
  grid-column: 1;
  -ms-grid-column: 1;
  grid-row: 2;
  -ms-grid-row: 2;

  // Fixes padding problem with padding-bottom from container
  > *:last-child {
    padding-bottom: 15px;;
  }
`;

const toggleSidebarPinning = (searchPageLayout) => {
  if (!searchPageLayout) {
    return;
  }

  const { actions: { toggleSidebarPinning: togglePinning } } = searchPageLayout;

  togglePinning();
};

const sidebarTitle = (viewMetadata: ViewMetadata, viewType: ViewType | undefined | null, viewIsNew: boolean) => {
  const viewTypeLabel = ViewTypeLabel({ type: viewType, capitalize: true });
  const unsavedViewTitle = `Unsaved ${viewTypeLabel}`;
  const savedViewTitle = viewMetadata.title ?? `Untitled ${viewTypeLabel}`;

  if (viewIsNew) {
    return unsavedViewTitle;
  }

  return savedViewTitle;
};

const ContentColumn = ({ children, sectionTitle, closeSidebar, searchPageLayout, viewMetadata, viewIsNew }: Props) => {
  const sidebarIsPinned = searchPageLayout?.config.sidebar.isPinned;

  return (
    <ViewTypeContext.Consumer>
      {(viewType) => {
        const title = sidebarTitle(viewMetadata, viewType, viewIsNew);

        return (
          <Container sidebarIsPinned={sidebarIsPinned}>

            <ContentGrid>
              <Header>
                <SearchTitle title={title}>
                  <CenterVertical>
                    <Title onClick={closeSidebar}>{title}</Title>
                  </CenterVertical>
                  <CenterVertical>
                    <OverlayToggle sidebarIsPinned={sidebarIsPinned}>
                      <IconButton onClick={() => toggleSidebarPinning(searchPageLayout)}
                                  title={`Display sidebar ${sidebarIsPinned ? 'as overlay' : 'inline'}`}
                                  name="thumbtack" />
                    </OverlayToggle>
                  </CenterVertical>
                </SearchTitle>
                <HorizontalRule />
                <SectionTitle>{sectionTitle}</SectionTitle>
              </Header>
              <SectionContent>
                {children}
              </SectionContent>
            </ContentGrid>
          </Container>
        );
      }}
    </ViewTypeContext.Consumer>
  );
};

export default ContentColumn;
