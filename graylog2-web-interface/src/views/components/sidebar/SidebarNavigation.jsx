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
  setActiveSectionKey: (sectionKey: string) => void,
  toggleSidebar: () => void,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => css`
  background: ${theme.colors.gray[10]};
  color: ${theme.utils.contrastingColor(theme.colors.gray[10], 'AA')};
  box-shadow: 3px 0 3px rgba(0, 0, 0, 0.25);
  width: 50px;
  height: 100%;
  position:relative;

  ::before {
    content: '';
    position: absolute;
    top: 0px;
    right: -4px;
    height: 4px;
    width: 4px;
    border-top-left-radius: 50%;
    background: transparent;
    box-shadow: -4px -4px 0px 4px ${theme.colors.gray[10]};
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

const SidebarNavigation = ({ sections, activeSection, setActiveSectionKey, toggleSidebar }: Props) => {
  const toggleIcon = activeSection ? 'chevron-left' : 'chevron-right';
  const activeSectionKey = activeSection?.key;

  return (
    <Container>
      <NavItem icon={toggleIcon}
               onClick={toggleSidebar}
               showTitleOnHover={false}
               title={`${activeSection ? 'Close' : 'Open'} sidebar`} />
      <HorizontalRuleWrapper><hr /></HorizontalRuleWrapper>
      <SectionList>
        {sections.map(({ key, icon, title }) => (
          <NavItem isSelected={activeSectionKey === key}
                   icon={icon}
                   onClick={() => setActiveSectionKey(key)}
                   key={key}
                   title={title} />
        ))}
      </SectionList>
    </Container>
  );
};

export default SidebarNavigation;
