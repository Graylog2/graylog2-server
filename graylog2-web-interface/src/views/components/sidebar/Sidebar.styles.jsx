// @flow strict
import styled, { type StyledComponent } from 'styled-components';
import { Title as NavItemTitle } from './NavItem.styles';

const sidebarWidth = {
  open: '250px',
  closed: '100%', // width is defined in parent container
};

export const Container: StyledComponent<{ open: boolean }, {}, HTMLDivElement> = styled.div`
  grid-row: 1;
  -ms-grid-row: 1;
  grid-column: 1;
  -ms-grid-column: 1;
  background: #393939;
  color: #9e9e9e;
  height: 100%;
  padding-top: 20px;
  position: ${props => (props.open ? 'fixed' : 'static')};
  top: 50px;
  width: ${props => (props.open ? sidebarWidth.open : sidebarWidth.closed)};
  box-shadow: 3px 0 3px rgba(0, 0, 0, 0.25);

  /* z-index is needed for ie11 */
  z-index: ${props => (props.open ? 20 : 'auto')};
`;

export const ContentOverlay: StyledComponent<{}, {}, HTMLDivElement> = styled.div`
  position: fixed;
  top: 0;
  bottom: 0;
  left: ${sidebarWidth.open};
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
