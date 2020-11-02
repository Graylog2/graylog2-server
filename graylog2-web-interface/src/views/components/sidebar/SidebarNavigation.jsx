// @flow strict
import * as React from 'react';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';

import NavItem from './NavItem';
import { type SidebarSection } from './sidebarSections';

type Props = {
  activeSection: ?SidebarSection,
  sections: Array<SidebarSection>,
  selectSidebarSection: (sectionKey: string) => void,
  sidebarIsPinned: boolean,
  toggleSidebar: () => void,
};

const Container: StyledComponent<{isOpen: boolean, sidebarIsPinned: boolean}, ThemeInterface, HTMLDivElement> = styled.div(({ isOpen, sidebarIsPinned, theme }) => css`
  background: ${theme.colors.global.navigationBackground};
  color: ${theme.utils.contrastingColor(theme.colors.global.navigationBackground, 'AA')};
  box-shadow: ${(sidebarIsPinned && isOpen) ? 'none' : `3px 3px 3px ${theme.colors.global.navigationBoxShadow}`};
  width: 50px;
  height: 100%;
  position:relative;
  z-index: 1031;

  ::before {
    content: '';
    position: absolute;
    top: 0px;
    right: -6px;
    height: 6px;
    width: 6px;
    border-top-left-radius: 50%;
    background: transparent;
    box-shadow: -6px -6px 0px 3px ${theme.colors.global.navigationBackground};
    z-index: 4; /* to render over Sidebar ContentColumn */
  }
`);

const SectionList = styled.div`
  > * {
    margin-bottom: 5px;

    :last-child {
      margin-bottom: 0;
    }
  }
`;

const HorizontalRuleWrapper = styled.div`
  padding: 0 10px;

  hr {
    border-color: currentColor;
    margin: 5px 0 10px 0;
  }
`;

const SidebarNavigation = ({ sections, activeSection, selectSidebarSection, sidebarIsPinned, toggleSidebar }: Props) => {
  const toggleIcon = activeSection ? 'chevron-left' : 'chevron-right';
  const activeSectionKey = activeSection?.key;

  return (
    <Container sidebarIsPinned={sidebarIsPinned} isOpen={!!activeSection}>
      <NavItem icon={toggleIcon}
               onClick={toggleSidebar}
               showTitleOnHover={false}
               title={`${activeSection ? 'Close' : 'Open'} sidebar`}
               sidebarIsPinned={sidebarIsPinned} />
      <HorizontalRuleWrapper><hr /></HorizontalRuleWrapper>
      <SectionList>
        {sections.map(({ key, icon, title }) => (
          <NavItem isSelected={activeSectionKey === key}
                   icon={icon}
                   onClick={() => selectSidebarSection(key)}
                   key={key}
                   title={title}
                   sidebarIsPinned={sidebarIsPinned} />
        ))}
      </SectionList>
    </Container>
  );
};

export default SidebarNavigation;
