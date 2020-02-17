// @flow strict
import styled, { type StyledComponent } from 'styled-components';
import { Title as NavItemTitle } from './NavItem.styles';

export const Container: StyledComponent<{ open: boolean }, {}, HTMLDivElement> = styled.div`
  grid-area: sidebar;
  z-index: 3;
  background: #393939;
  color: #9e9e9e;
  height: calc(100vh - 50px);
  padding-top: 20px;
  position: sticky;
  top: 50px;
  grid-column-start: 1;
  grid-column-end: ${props => (props.open ? 3 : 2)};
  box-shadow: 3px 0 3px rgba(0, 0, 0, .25);
`;

export const ContentOverlay: StyledComponent<{}, {}, HTMLDivElement> = styled.div`
  position: fixed;
  top: 0;
  bottom: 0;
  left: 300px;
  right: 0;
  background: rgba(3, 3, 3, 0.25);
`;

export const SidebarHeader: StyledComponent<{open: boolean, hasTitle: boolean}, {}, React.ComponentType> = styled(NavItemTitle)`
  ${(({ open, hasTitle }) => {
    let justifyContent = 'center';
    if (open && hasTitle) justifyContent = 'space-between';
    if (open && !hasTitle) justifyContent = 'flex-end';
    return `justify-content: ${justifyContent}`;
  })}
`;

export const Headline: StyledComponent<{}, {}, HTMLHeadingElement> = styled.h3`
  color: inherit;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const ToggleIcon: StyledComponent<{}, {}, HTMLDivElement> = styled.div`
  width: 25px;
  text-align: center;
  font-size: 20px;
  cursor: pointer;
`;

export const HorizontalRuler: StyledComponent<{}, {}, HTMLDivElement> = styled.div`
  width: 100%;
  padding: 0 10px;
  margin: 5px 0 10px 0;

  &::after {
    content: ' ';
    display: block;
    width: 100%;
    border-bottom: 1px solid currentColor;
  }
`;
