// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import { Icon } from 'components/common';
import { type ThemeInterface } from 'theme';

type Props = {
  isSelected: boolean,
  title: string,
  icon: string,
  onClick: () => void,
  showTitleOnHover: boolean,
};

const Title = styled.div`
  padding: 5px 10px;
  position: absolute;
  left: 100%;
  background-color: grey;
  z-index: 4;
  width: max-content;
  font-size: 14px;
  color: white;
  display: none;
`;

const Container: StyledComponent<{ isSelected: boolean, showTitleOnHover: boolean }, ThemeInterface, HTMLDivElement> = styled.div(({ theme, isSelected, showTitleOnHover }) => `
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  width: 100%;
  height: 40px;

  text-align: center;
  cursor: pointer;
  font-size: 20px;
  color: ${isSelected ? theme.colors.variant.light.danger : 'inherit'};
  cursor: pointer;
  
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
    display: none;
    position: absolute;
    right: 0;
    width: 0;
    height: 0;
    border-top: 10px solid transparent;
    border-right: 10px solid white;
    border-bottom: 10px solid transparent;
  }

  ${(isSelected ? `
    &::after {
      display: block;
    }
  ` : '')}

  ${((!isSelected && showTitleOnHover) ? `
    :hover::after {
      display: block;
      border-right-color: grey;
      
    }
  ` : '')}
`);

const NavItem = ({ isSelected, title, icon, onClick, showTitleOnHover }: Props) => {
  return (
    <Container isSelected={isSelected}
               onClick={onClick}
               showTitleOnHover={showTitleOnHover}
               title={title}>
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
