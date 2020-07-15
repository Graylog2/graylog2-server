// @flow strict
import * as React from 'react';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

import View, { type ViewType } from 'views/logic/views/View';
import type { ThemeInterface } from 'theme';
import { type SearchPageLayout } from 'views/components/contexts/SearchPageLayoutContext';
import { type ViewMetaData as ViewMetadata } from 'views/stores/ViewMetadataStore';
import { IconButton } from 'components/common';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

type Props = {
  children: React.Node,
  closeSidebar: () => void,
  searchPageLayout: ?SearchPageLayout,
  sectionTitle: string,
  viewIsNew: boolean,
  viewMetadata: ViewMetadata,
};

export const Container: StyledComponent<{ sidebarIsPinned: boolean }, ThemeInterface, HTMLDivElement> = styled.div(({ theme, sidebarIsPinned }) => css`
  position: ${sidebarIsPinned ? 'relative' : 'fixed'};
  display: grid;
  display: -ms-grid;
  grid-template-columns: 1fr;
  grid-template-rows: auto 1fr;
  -ms-grid-columns: 1fr;
  -ms-grid-rows: auto 1fr;
  top: ${sidebarIsPinned ? 0 : '50px'};
  left: ${sidebarIsPinned ? 0 : '50px'};

  width: 270px;
  height: 100%;
  padding: 5px 15px 15px 15px;

  color: ${theme.colors.global.textDefault};
  background: ${theme.colors.global.contentBackground};
  border-right: 1px solid ${theme.colors.gray[80]};
  box-shadow: ${sidebarIsPinned ? '3px 3px 3px rgba(0, 0, 0, 0.25)' : 'none'};

  z-index: ${sidebarIsPinned ? 1030 : 3};

  ${sidebarIsPinned ? css`
    ::before {
      content: '';
      position: absolute;
      top: 0px;
      right: -6px;
      height: 6px;
      width: 6px;
      border-top-left-radius: 50%;
      background: transparent;
      box-shadow: -6px -6px 0px 3px ${theme.colors.global.contentBackground};
      z-index: 4; /* to render over Sidebar ContentColumn */
    }
  ` : ''}

  > *:nth-child(1) {
    grid-column: 1;
    -ms-grid-column: 1;
    grid-row: 1;
    -ms-grid-row: 1;
  }

  > *:nth-child(2) {
    grid-column: 1;
    -ms-grid-column: 1;
    grid-row: 2;
    -ms-grid-row: 2;
  }
`);

const Header: StyledComponent<{}, void, HTMLDivElement> = styled.div`
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

const toggleSidebarPinning = (searchPageLayout) => {
  if (!searchPageLayout) {
    return;
  }

  const { actions: { toggleSidebarPinning: togglePinning } } = searchPageLayout;

  togglePinning();
};

const sidebarTitle = (viewMetadata: ViewMetadata, viewType: ?ViewType, viewIsNew: boolean) => {
  const viewTypeLabel = ViewTypeLabel({ type: viewType ?? View.Type.Search, capitalize: true });
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
            <div>
              <Header title={title}>
                <CenterVertical>
                  <Title onClick={closeSidebar}>{title}</Title>
                </CenterVertical>
                <CenterVertical>
                  <OverlayToggle sidebarIsPinned={sidebarIsPinned}>
                    <IconButton onClick={() => toggleSidebarPinning(searchPageLayout)}
                                title={`Display sidebar ${sidebarIsPinned ? 'as overlay' : 'inline'}`}
                                name="thumb-tack" />
                  </OverlayToggle>
                </CenterVertical>
              </Header>
              <HorizontalRule />
              <SectionTitle>{sectionTitle}</SectionTitle>
            </div>
            <div>
              {children}
            </div>
          </Container>
        );
      }}
    </ViewTypeContext.Consumer>
  );
};

export default ContentColumn;
