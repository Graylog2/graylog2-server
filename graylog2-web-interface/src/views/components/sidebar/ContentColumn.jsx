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
  top: ${sidebarIsInline ? 0 : '50px'};
  left: ${sidebarIsInline ? 0 : '50px'};

  width: 270px;
  height: 100%;
  padding: 5px 15px;

  color: ${theme.colors.global.textDefault};
  background: ${theme.colors.global.contentBackground};
  border-right: 1px solid ${theme.colors.gray[80]};
  
  overflow-y: auto;
  z-index: 3;
`);

const Header: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  height: 35px;
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

const Title = styled.h1`
  color: inherit;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 24px;
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
  const defaultViewTitle = `Untitled ${viewType ? ViewTypeLabel({ type: viewType }) : View.Type.Search}`;
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
            <Header title={title}>
              <Title onClick={closeSidebar}>{title}</Title>
              <OverlayToggle sidebarIsInline={sidebarIsInline}>
                <IconButton onClick={() => toggleSidebarPinning(searchPageLayout)}
                            title={`Display sidebar ${sidebarIsInline ? 'as overlay' : 'inline'}`}
                            name="layer-group" />
              </OverlayToggle>
            </Header>
            <HorizontalRule />
            <SectionTitle>{section.title}</SectionTitle>
            <Content {...sectionProps} />
          </Container>
        );
      }}
    </ViewTypeContext.Consumer>
  );
};

export default ContentColumn;
