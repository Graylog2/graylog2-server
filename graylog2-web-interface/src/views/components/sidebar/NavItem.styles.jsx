// @flow strict
import styled, { type StyledComponent, css } from 'styled-components';
import { type ThemeInterface } from 'theme';

type StyleProps = {
  isSelected: boolean,
  expandRight: boolean,
};

export const Title: StyledComponent<StyleProps, ThemeInterface, HTMLDivElement> = styled.div(({ isSelected, expandRight, theme }) => css`
  padding: 9px 10px;
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
  color: ${isSelected ? theme.colors.variant.light.danger : 'inherit'};
  background: ${isSelected ? theme.colors.gray[10] : 'tranparent'};
  ${((isSelected && expandRight) && css`

    &::after {
      content: ' ';
      display: block;
      position: absolute;
      right: 0;
      width: 0;
      height: 0;
      border-top: 15px solid transparent;
      border-right: 15px solid white;
      border-bottom: 15px solid transparent;
    }
  `)}
`);

export const TitleText: StyledComponent<{}, {}, HTMLDivElement> = styled.div`
  font-size: 16px;
  display: inline;
  margin-left: 10px;
  overflow: hidden;
  white-space: nowrap;
`;

export const TitleIcon: StyledComponent<{}, {}, HTMLDivElement> = styled.div`
  width: 25px;
  text-align: center;
  font-size: 20px;
  cursor: pointer;
`;

export const Content: StyledComponent<StyleProps, ThemeInterface, HTMLDivElement> = styled.div(({ isSelected, expandRight, theme, sidebarIsPinned }) => css`
  color: ${theme.utils.readableColor(theme.colors.global.contentBackground)};
  background: ${theme.colors.global.contentBackground};
  box-shadow:
    inset 0 13px 5px -10px ${theme.colors.gray[80]},
    inset 0 -13px 5px -10px ${theme.colors.gray[80]};
  ${(isSelected ? css`
    padding: 20px;
  ` : css`
    max-height: 0;
  `)}
  ${(isSelected && expandRight) && css`
    position: absolute !important;
    top: ${sidebarIsPinned ? '50px' : 0};
    left: ${sidebarIsPinned ? '50px' : '100%'};
    border: 0;
    bottom: 0;
    padding: 20px;
    width: 270px;
    overflow-y: hidden;
  `}
`);
