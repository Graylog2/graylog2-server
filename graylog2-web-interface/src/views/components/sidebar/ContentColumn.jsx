// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import View, { type ViewType } from 'views/logic/views/View';
import { type ThemeInterface } from 'theme';
import { type SearchPageLayout } from 'views/components/contexts/SearchPageLayoutContext';
import { type ViewMetaData as ViewMetadata } from 'views/stores/ViewMetadataStore';

import ViewTypeLabel from 'views/components/ViewTypeLabel';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import { IconButton } from 'components/common';

import type { SidebarSection, SidebarSectionProps } from './sidebarSections';

type Props = {
  section: SidebarSection,
  closeSidebar: () => void,
  sectionProps: SidebarSectionProps,
  searchPageLayout: ?SearchPageLayout,
  viewMetadata: ViewMetadata,
};

export const Container: StyledComponent<{ sidebarIsInline: boolean }, ThemeInterface, HTMLDivElement> = styled.div(({ theme, sidebarIsInline }) => `
  position: ${sidebarIsInline ? 'static' : 'fixed'}
  display: grid;
  display: -ms-grid;
  grid-templage-columns: 1fr;
  grid-template-rows: auto 1fr;
  -ms-grid-columns: 1fr;
  -ms-grid-rows: auto 1fr;
  top: ${sidebarIsInline ? 0 : '50px'};
  left: ${sidebarIsInline ? 0 : '50px'};

  width: 270px;
  height:  ${sidebarIsInline ? '100%' : 'calc(100% - 50px)'};;
  padding: 5px 15px 15px 15px;

  color: ${theme.colors.global.textDefault};
  background: ${theme.colors.global.contentBackground};
  border-right: 1px solid ${theme.colors.gray[80]};
  
  overflow-y: auto;
  z-index: 3;

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
  font-size: 24px;
  cursor: pointer;
`;

const OverlayToggle: StyledComponent<{ sidebarIsInline: boolean }, ThemeInterface, HTMLDivElement> = styled.div(({ theme, sidebarIsInline }) => `
  > * {
    font-size: 18px;
    color: ${sidebarIsInline ? theme.colors.gray[30] : theme.colors.variant.info};
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
  const { setConfig, config } = searchPageLayout;
  const sidebarIsInline = config?.sidebar.isInline;
  const newLayoutConfig = {
    ...config,
    sidebar: { isInline: !sidebarIsInline },
  };
  setConfig(newLayoutConfig);
};

const sidebarTitle = (viewMetadata: ViewMetadata, viewType: ?ViewType) => {
  if (!viewMetadata.id) {
    return 'Ad Hoc Search';
  }
  const defaultViewTitle = `Untitled ${viewType ? ViewTypeLabel({ type: viewType, capitalize: true }) : View.Type.Search}`;
  return viewMetadata.title || defaultViewTitle;
};

const ContentColumn = ({ section, closeSidebar, sectionProps, searchPageLayout, viewMetadata }: Props) => {
  const sidebarIsInline = searchPageLayout?.config.sidebar.isInline;
  const Content = section.content;
  return (
    <ViewTypeContext.Consumer>
      {(viewType) => {
        const title = sidebarTitle(viewMetadata, viewType);
        return (
          <Container sidebarIsInline={sidebarIsInline}>
            <div>
              <Header title={title}>
                <CenterVertical>
                  <Title onClick={closeSidebar}>{title}</Title>
                </CenterVertical>
                <CenterVertical>
                  <OverlayToggle sidebarIsInline={sidebarIsInline}>
                    <IconButton onClick={() => toggleSidebarPinning(searchPageLayout)}
                                title={`Display sidebar ${sidebarIsInline ? 'as overlay' : 'inline'}`}
                                name="layer-group" />
                  </OverlayToggle>
                </CenterVertical>
              </Header>
              <HorizontalRule />
              <SectionTitle>{section.title}</SectionTitle>
            </div>
            <div>
              <Content {...sectionProps} />
            </div>
          </Container>
        );
      }}
    </ViewTypeContext.Consumer>
  );
};

export default ContentColumn;
