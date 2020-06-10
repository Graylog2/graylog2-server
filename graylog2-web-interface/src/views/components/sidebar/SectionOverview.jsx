// @flow strict
import * as React from 'React';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';

import HorizontalRule from './HorizontalRule';
import NavItem from './NavItem';

import { type SidebarSection } from './sidebarSections';

type Props = {
  activeSection: ?SidebarSection,
  sections: Array<SidebarSection>,
  setActiveSectionKey: (sectionKey: string) => void,
  toggleSidebar: () => void,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  background: ${theme.colors.gray[10]};
  color: ${theme.utils.contrastingColor(theme.colors.gray[10], 'AA')};
  box-shadow: 3px 0 3px rgba(0, 0, 0, 0.25);
  width: 50px;
  height: 100%;
`);

const SidebarToggle: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  > * {
    font-size: 26px;
  }
`;

const SectionList = styled.div`
  > * {
    margin-bottom: 5px;

    :last-child {
      margin-bottom: 0;
    }
  }
`;

const SectionOverview = ({ sections, activeSection, setActiveSectionKey, toggleSidebar }: Props) => {
  const toggleIcon = activeSection ? 'angle-left' : 'angle-right';
  const activeSectionKey = activeSection?.key;

  return (
    <Container>
      <SidebarToggle>
        <NavItem icon={toggleIcon} title={`${activeSection ? 'Close' : 'Open'} sidebar`} onClick={() => toggleSidebar()} showTitleOnHover={false} />
      </SidebarToggle>
      <HorizontalRule />
      <SectionList>
        {sections.map((section) => (
          <NavItem isSelected={activeSectionKey === section.key}
                   title={section.title}
                   icon={section.icon}
                   onClick={() => setActiveSectionKey(section.key)} />
        ))}
      </SectionList>
    </Container>
  );
};


export default SectionOverview;
