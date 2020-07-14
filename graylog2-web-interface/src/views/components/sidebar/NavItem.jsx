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
};

const Title: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => css`
  display: none;
  position: absolute;
  padding: 5px 10px;
  left: 100%;
  top: 5px;
  height: 30px;
  font-size: ${theme.fonts.size.body};
  color: white;
  background-color: ${theme.utils.contrastingColor(theme.colors.gray[10], 'AA')};
  z-index: 4;
`);

const IconWrap: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => css`
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  position: relative;
`);

const Container: StyledComponent<{ isSelected: boolean, showTitleOnHover: boolean }, ThemeInterface, HTMLDivElement> = styled.div(({ theme, isSelected, showTitleOnHover }) => css`
  position: relative;
  z-index: 4; /* to render over SidebarNav::before */
  width: 100%;
  height: 40px;
  text-align: center;
  cursor: pointer;
  font-size: ${theme.fonts.size.h3};
  color: ${isSelected ? theme.colors.variant.dark.primary : theme.colors.variant.darker.info};
  background: ${isSelected ? theme.colors.variant.lighter.info : 'transparent'};

  :hover {
    background: ${isSelected ? theme.colors.variant.lighter.info : theme.colors.variant.lightest.info};

    ${Title} {
      display: ${(!isSelected && showTitleOnHover) ? 'block' : 'none'};
    }

    ${IconWrap}::after {
      display: ${(showTitleOnHover) ? 'block' : 'none'};
    }
  }

  :active {
    background: ${theme.colors.variant.lighter.info};
  }

  ${IconWrap} {
    overflow: ${isSelected ? 'unset' : 'hidden'};

    ::after {
      content: ' ';
      display: ${isSelected ? 'block' : 'none'};
      position: absolute;
      right: -8px;
      top: calc(50% - 8px);
      width: 16px;
      height: 16px;
      transform: rotate(45deg);
      box-shadow: ${isSelected ? 'inset 3px -3px 2px 0px rgba(0,0,0,0.25)' : 'none'};
      background-color: ${isSelected ? theme.colors.global.contentBackground : theme.colors.variant.light.info};
    }
  }
`);

const NavItem = ({ isSelected, title, icon, onClick, showTitleOnHover }: Props) => {
  return (
    <Container aria-label={title}
               isSelected={isSelected}
               onClick={onClick}
               showTitleOnHover={showTitleOnHover}
               title={showTitleOnHover ? '' : title}>
      <IconWrap><Icon name={icon} /></IconWrap>
      {(showTitleOnHover && !isSelected) && <Title>{title}</Title>}
    </Container>
  );
};

NavItem.propTypes = {
  icon: PropTypes.node.isRequired,
  isSelected: PropTypes.bool,
  showTitleOnHover: PropTypes.bool,
  title: PropTypes.string.isRequired,
};

NavItem.defaultProps = {
  isSelected: false,
  showTitleOnHover: true,
};

export default NavItem;
