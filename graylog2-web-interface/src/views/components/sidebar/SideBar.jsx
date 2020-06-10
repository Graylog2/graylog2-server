// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import chroma from 'chroma-js';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';
import { isString } from 'lodash';

import { type ThemeInterface } from 'theme';
import { type ViewMetaData as ViewMetadata } from 'views/stores/viewMetadataStore';
import SearchPageLayoutContext, { type SearchPageLayoutType } from 'views/components/contexts/SearchPageLayoutContext';
import CustomPropTypes from '../CustomPropTypes';
import SectionOverview from './SectionOverview';
import SectionContent from './SectionContent';
import sidebarSections, { type SidebarSection } from './sidebarSections';

type Props = {
  children: React.Element<any>,
  disableAutoClose: boolean,
  queryId: string,
  results: {},
  searchPageLayout: ?SearchPageLayoutType,
  sections: Array<SidebarSection>,
  viewMetadata: ViewMetadata,
};

const Container = styled.div`
  display: flex;
  grid-row: 1;
  -ms-grid-row: 1;
  grid-column: 1;
  -ms-grid-column: 1;
`;

const ContentOverlay: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  position: fixed;
  top: 0;
  bottom: 0;
  left: 50px;
  right: 0;
  background: ${chroma(theme.colors.brand.tertiary).alpha(0.25).css()};
  z-index: 2;  
`);

const handleClickOutside = (event: MouseEvent, activeSectionKey: ?string, toggleSidebar: () => void, disableAutoClose: boolean) => {
  // $FlowFixMe: EventTarget and className work here.
  const { className } = event.target;
  const canMatchClass = className && isString(className);
  if (activeSectionKey && !disableAutoClose && (canMatchClass && className.match(/background/))) {
    toggleSidebar();
  }
};
const handleToggleSidebar = (sections: Array<SidebarSection>, activeSectionKey: ?string, setActiveSectionKey) => {
  if (activeSectionKey) {
    setActiveSectionKey(null);
    return;
  }
  setActiveSectionKey(sections[0].key);
};

const Sidebar = ({ searchPageLayout, results, children, queryId, disableAutoClose, sections, viewMetadata }: Props) => {
  const [activeSectionKey, setActiveSectionKey] = useState<?string>(null);
  const activeSection = sections.find((section) => section.key === activeSectionKey);
  const isSidebarPinned = searchPageLayout?.layout.sidebar.pinned;
  const toggleSidebar = () => handleToggleSidebar(sections, activeSectionKey, setActiveSectionKey);
  const onClickOutside = (event) => handleClickOutside(event, activeSectionKey, toggleSidebar, disableAutoClose);

  useEffect(() => {
    document.addEventListener('mousedown', onClickOutside);
    return document.removeEventListener('mousedown', onClickOutside);
  }, []);

  return (
    <Container>
      <SectionOverview activeSection={activeSection}
                       setActiveSectionKey={setActiveSectionKey}
                       toggleSidebar={toggleSidebar}
                       sections={sections} />
      {activeSection && (
        <SectionContent closeSidebar={toggleSidebar}
                        isPinned={!!isSidebarPinned}
                        searchPageLayout={searchPageLayout}
                        section={activeSection}
                        sectionProps={{ results, children, queryId, toggleSidebar, viewMetadata }}
                        viewMetadata={viewMetadata} />
      )}
      {(activeSection && !isSidebarPinned) && (
        <ContentOverlay onClick={toggleSidebar} />
      )}
    </Container>
  );
};

Sidebar.propTypes = {
  children: CustomPropTypes.OneOrMoreChildren.isRequired,
  disableAutoClose: PropTypes.bool,
  queryId: PropTypes.string.isRequired,
  results: PropTypes.object,
  sections: PropTypes.arrayOf(PropTypes.object),
  viewMetadata: PropTypes.shape({
    activeQuery: PropTypes.string,
    description: PropTypes.string,
    id: PropTypes.string,
    summary: PropTypes.string,
    title: PropTypes.string,
  }).isRequired,
};

Sidebar.defaultProps = {
  disableAutoClose: false,
  results: {},
  sections: sidebarSections,
};

const SidebarWithContext = (props: Props) => (
  <SearchPageLayoutContext.Consumer>
    {(searchPageLayout) => {
      return <Sidebar {...props} searchPageLayout={searchPageLayout} />;
    }}
  </SearchPageLayoutContext.Consumer>
);

export default SidebarWithContext;
