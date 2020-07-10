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
  font-size: ${theme.fonts.size.body};
  color: white;
  background-color: ${theme.utils.contrastingColor(theme.colors.gray[10], 'AA')};
  z-index: 4;
`);

const Container: StyledComponent<{ isSelected: boolean, showTitleOnHover: boolean }, ThemeInterface, HTMLDivElement> = styled.div(({ theme, isSelected, showTitleOnHover }) => css`
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 4; /* to render over SidebarNav::before */
  width: 100%;
  height: 40px;

  text-align: center;
  cursor: pointer;
  font-size: ${theme.fonts.size.h3};
  color: ${isSelected ? theme.colors.variant.light.danger : 'inherit'};

  :hover {
    background: ${theme.colors.gray[30]};

    > * {
      display: block;
    }
  }

  :active {
    background: ${theme.colors.gray[20]};
  }

  &::after {
    content: ' ';
    display: ${isSelected ? 'block' : 'none'};
    position: absolute;
    right: 0;
    width: 0;
    height: 0;
    border-top: 10px solid transparent;
    border-right: 10px solid white;
    border-bottom: 10px solid transparent;
  }

  ${((!isSelected && showTitleOnHover) ? `
    :hover::after {
      display: block;
      border-right-color: currentColor;
    }

    :active {
      &::after, > div {
        display: none;
      }
    }
  ` : '')}
`);

const NavItem = ({ isSelected, title, icon, onClick, showTitleOnHover }: Props) => {
  return (
    <Container aria-label={title}
               isSelected={isSelected}
               onClick={onClick}
               showTitleOnHover={showTitleOnHover}
               title={showTitleOnHover ? '' : title}>
      <Icon name={icon} />
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
