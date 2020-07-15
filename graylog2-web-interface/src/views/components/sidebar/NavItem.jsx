// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { Icon } from 'components/common';

type Props = {
  isSelected: boolean,
  title: string,
  icon: string,
  onClick: () => void,
  showTitleOnHover: boolean,
  sidebarIsPinned: boolean,
};

type ContainerProps = {
  isSelected: boolean,
  showTitleOnHover: boolean,
  sidebarIsPinned: boolean,
};

const Title: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => css`
  display: none;
  position: absolute;
  padding: 0 10px;
  left: 100%;
  top: calc(50% - 10px);
  height: 21px;
  background-color: ${theme.colors.variant.lighter.info};
  box-shadow: 3px 3px 3px ${theme.colors.global.navigationBoxShadow};
  z-index: 4;
  border-radius: 0 3px 3px 0;

  span {
    background: ${theme.utils.contrastingColor(theme.colors.variant.lighter.info, 'AA')};
    font-size: ${theme.fonts.size.body};
    font-weight: bold;
    background-clip: text;
    text-shadow: 0 1px 2px rgba(255, 255, 255, 0.35);
    text-transform: uppercase;
  }
`);

const IconWrap: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span`
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  position: relative;

  ::after {
    content: ' ';
    position: absolute;
    right: -7px;
    top: calc(50% - 7px);
    width: 14px;
    height: 14px;
    transform: rotate(45deg);
  }
`;

const Container: StyledComponent<ContainerProps, ThemeInterface, HTMLDivElement> = styled.div(({ theme, isSelected, showTitleOnHover, sidebarIsPinned }) => css`
  position: relative;
  z-index: 4; /* to render over SidebarNav::before */
  width: 100%;
  height: 40px;
  text-align: center;
  cursor: pointer;
  font-size: ${theme.fonts.size.h3};
  color: ${isSelected ? theme.colors.variant.dark.default : theme.colors.variant.darker.default};
  background: ${isSelected ? theme.colors.variant.lighter.default : 'transparent'};

  :hover {
    background: ${isSelected ? theme.colors.variant.lighter.default : theme.colors.variant.lightest.default};

    ${Title} {
      display: ${(showTitleOnHover && !isSelected) ? 'flex' : 'none'};
    }

    ${IconWrap}::after {
      display: ${(showTitleOnHover) ? 'block' : 'none'};
    }
  }

  :active {
    background: ${theme.colors.variant.lighter.default};
  }

  ${IconWrap} {
    overflow: ${isSelected ? 'unset' : 'hidden'};

    ::after {
      display: ${isSelected ? 'block' : 'none'};
      box-shadow: ${(isSelected && !sidebarIsPinned) ? `3px -3px 2px 0 ${theme.colors.global.navigationBoxShadow}` : 'none'};
      background-color: ${isSelected ? theme.colors.variant.lighter.default : theme.colors.variant.lighter.info};
    }
  }
`);

const NavItem = ({ isSelected, title, icon, onClick, showTitleOnHover, sidebarIsPinned }: Props) => {
  return (
    <Container aria-label={title}
               isSelected={isSelected}
               onClick={onClick}
               showTitleOnHover={showTitleOnHover}
               title={showTitleOnHover ? '' : title}
               sidebarIsPinned={sidebarIsPinned}>
      <IconWrap><Icon name={icon} /></IconWrap>
      {(showTitleOnHover && !isSelected) && <Title><span>{title}</span></Title>}
    </Container>
  );
};

NavItem.propTypes = {
  icon: PropTypes.node.isRequired,
  isSelected: PropTypes.bool,
  showTitleOnHover: PropTypes.bool,
  sidebarIsPinned: PropTypes.bool.isRequired,
  title: PropTypes.string.isRequired,
};

NavItem.defaultProps = {
  isSelected: false,
  showTitleOnHover: true,
};

export default NavItem;
