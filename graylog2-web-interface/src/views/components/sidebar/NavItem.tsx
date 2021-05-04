/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

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

const Title = styled.div(({ theme }) => css`
  display: none;
  position: absolute;
  padding: 0 10px;
  left: 100%;
  top: calc(50% - 13px);
  height: 25px;
  background-color: ${theme.colors.variant.lightest.info};
  border: 1px solid ${theme.colors.variant.light.info};
  border-left: none;
  box-shadow: 3px 3px 3px ${theme.colors.global.navigationBoxShadow};
  z-index: 4;
  border-radius: 0 3px 3px 0;
  align-items: center;

  span {
    color: ${theme.colors.variant.darker.info};
    font-size: ${theme.fonts.size.body};
    font-weight: bold;
    text-transform: uppercase;
  }
`);

const IconWrap = styled.span`
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  position: relative;

  ::after {
    content: ' ';
    position: absolute;
    left: 82.5%;
    top: calc(50% - 9px);
    width: 18px;
    height: 18px;
    transform: rotate(45deg);
  }
`;

const Container = styled.div<ContainerProps>(({ theme, isSelected, showTitleOnHover, sidebarIsPinned }) => css`
  position: relative;
  z-index: 4; /* to render over SidebarNav::before */
  width: 100%;
  height: 40px;
  text-align: center;
  cursor: pointer;
  font-size: ${theme.fonts.size.h3};
  color: ${theme.colors.variant.darkest.default};
  background: ${isSelected ? theme.colors.gray[90] : theme.colors.global.contentBackground};

  :hover {
    color: ${isSelected ? theme.colors.variant.darkest.default : theme.colors.variant.darker.default};
    background: ${isSelected ? theme.colors.gray[80] : theme.colors.variant.lightest.default};

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
  /* stylelint-disable selector-max-empty-lines, indentation */
  ${(isSelected && !sidebarIsPinned) && css`
    ::before,
    ::after {
      content: '';
      position: absolute;
      right: -5px;
      height: 15px;
      width: 5px;
      background-color: ${theme.colors.global.contentBackground};
    }

    ::before {
      transform: skewY(-45deg);
      top: calc(50% - 12px);
    }
    
    ::after {
      transform: skewY(45deg);
      bottom: calc(50% - 12px);
    }
  `}
  /* stylelint-enable selector-max-empty-lines, indentation */

  ${IconWrap} {
    overflow: hidden;

    ::after {
      display: ${isSelected ? 'block' : 'none'};
      box-shadow: ${(isSelected && !sidebarIsPinned) ? `inset 2px -2px 2px 0 ${theme.colors.global.navigationBoxShadow}` : 'none'};
      background-color: ${isSelected ? theme.colors.global.contentBackground : theme.colors.variant.lightest.info};
      border: ${isSelected ? 'none' : `1px solid ${theme.colors.variant.light.info}`};
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
