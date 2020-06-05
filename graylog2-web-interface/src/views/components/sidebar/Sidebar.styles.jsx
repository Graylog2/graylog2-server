// @flow strict
import styled, { css, type StyledComponent } from 'styled-components';
import chroma from 'chroma-js';
import { type ThemeInterface } from 'theme';
import { Title as NavItemTitle } from './NavItem.styles';

const sidebarWidth = {
  open: '250px',
  closed: '50px', // width is defined in parent container
};

export const Container: StyledComponent<{ open: boolean }, ThemeInterface, HTMLDivElement> = styled.div(({ open, theme, isPinned }) => css`
  grid-row: 1;
  -ms-grid-row: 1;
  grid-column: 1;
  -ms-grid-column: 1;
  background: ${theme.colors.gray[10]};
  color: ${theme.utils.contrastingColor(theme.colors.gray[10], 'AA')};
  height: calc(100vh - 50px);
  padding-top: 20px;
  position: ${open && !isPinned ? 'fixed' : 'static'};
  top: 50px;
  width: ${(open && !isPinned) ? sidebarWidth.open : sidebarWidth.closed};
  box-shadow: 3px 0 3px rgba(0, 0, 0, 0.25);
  margin-right: ${isPinned ? '270px' : 0};

  /* z-index is needed for ie11 */
  z-index: ${open ? 20 : 'auto'};
`);

export const ContentOverlay: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  position: fixed;
  top: 0;
  bottom: 0;
  left: ${sidebarWidth.open};
  right: 0;
  background: ${chroma(theme.colors.brand.tertiary).alpha(0.25).css()};
`);

export const SidebarHeader: StyledComponent<{open: boolean, hasTitle: boolean}, {}, typeof NavItemTitle> = styled(NavItemTitle)(({ hasTitle, open }) => {
  const justifyContent = (open && !hasTitle) ? 'flex-end' : 'center';

  return `
    justify-content: ${(open && hasTitle) ? 'space-between' : justifyContent}
  `;
});

export const Headline: StyledComponent<{}, void, HTMLHeadingElement> = styled.h3`
  color: inherit;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const ToggleIcon: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  width: 25px;
  text-align: center;
  font-size: 20px;
  cursor: pointer;
`;

export const HorizontalRuler: StyledComponent<{}, void, HTMLDivElement> = styled.div`
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
