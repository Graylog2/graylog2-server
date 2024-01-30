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

import Icon from 'components/common/Icon';
import type { IconName } from 'components/common/Icon';

export type NavItemProps = {
  isSelected: boolean,
  title: string,
  icon: IconName,
  onClick: () => void,
  showTitleOnHover?: boolean,
  sidebarIsPinned: boolean,
  disabled?: boolean,
  ariaLabel: string,
};

type ContainerProps = {
  $isSelected: boolean,
  $sidebarIsPinned: boolean,
  $disabled: boolean,
};

const Container = styled.button<ContainerProps>(({ theme: { colors, fonts }, $isSelected, $sidebarIsPinned, $disabled }) => css`
  position: relative;
  z-index: 4; /* to render over SidebarNav::before */
  width: 100%;
  height: 40px;
  text-align: center;
  cursor: ${$disabled ? 'not-allowed' : 'pointer'};
  font-size: ${fonts.size.h3};
  color: ${colors.variant.darkest.default};
  background: ${$isSelected ? colors.gray[90] : colors.global.contentBackground};
  border: 0;
  padding: 0;

  &:hover {
    color: ${$isSelected ? colors.variant.darkest.default : colors.variant.darker.default};
    background: ${$isSelected ? colors.gray[80] : colors.variant.lightest.default};
  }

  &:active {
    background: ${colors.variant.lighter.default};
  }

  /* stylelint-disable selector-max-empty-lines, indentation */
  ${($isSelected && !$sidebarIsPinned) && css`
    &::before,
    &::after {
      content: '';
      position: absolute;
      right: -5px;
      height: 15px;
      width: 5px;
      background-color: ${colors.global.contentBackground};
    }

    &::before {
      transform: skewY(-45deg);
      top: calc(50% - 12px);
    }
    
    &::after {
      transform: skewY(45deg);
      bottom: calc(50% - 12px);
    }
`}
  /* stylelint-enable selector-max-empty-lines, indentation */
`);

type IconWrapProps = {
  $showTitleOnHover: boolean,
  $isSelected: boolean,
  $sidebarIsPinned: boolean,
  $disabled: boolean,
}
const IconWrap = styled.span<IconWrapProps>(({ $showTitleOnHover, $isSelected, $disabled, $sidebarIsPinned, theme: { colors } }) => css`
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  position: relative;
  opacity: ${$disabled ? 0.65 : 1};

  &:hover {
    + div {
      display: ${($showTitleOnHover && !$isSelected) ? 'flex' : 'none'};
    }

    &::after {
      display: ${($showTitleOnHover) ? 'block' : 'none'};
    }
  }

  &::after {
    display: ${$isSelected ? 'block' : 'none'};
    box-shadow: ${($isSelected && !$sidebarIsPinned) ? `inset 2px -2px 2px 0 ${colors.global.navigationBoxShadow}` : 'none'};
    background-color: ${$isSelected ? colors.global.contentBackground : colors.variant.lightest.info};
    border: ${$isSelected ? 'none' : `1px solid ${colors.variant.light.info}`};
    content: ' ';
    position: absolute;
    left: 82.5%;
    top: calc(50% - 9px);
    width: 18px;
    height: 18px;
    transform: rotate(45deg);
  }
`);

const Title = styled.div(({ theme: { colors, fonts } }) => css`
  display: none;
  position: absolute;
  padding: 0 10px;
  left: 100%;
  top: calc(50% - 13px);
  height: 26px;
  background-color: ${colors.variant.lightest.info};
  border: 1px solid ${colors.variant.light.info};
  border-left: none;
  box-shadow: 3px 3px 3px ${colors.global.navigationBoxShadow};
  z-index: 4;
  border-radius: 0 3px 3px 0;
  align-items: center;

  span {
    color: ${colors.variant.darker.info};
    font-size: ${fonts.size.body};
    font-weight: bold;
    text-transform: uppercase;
  }
`);

const NavItem = ({ isSelected, title, icon, onClick, showTitleOnHover, sidebarIsPinned, disabled, ariaLabel }: NavItemProps) => (
  <Container aria-label={ariaLabel}
             $isSelected={isSelected}
             onClick={!disabled ? onClick : undefined}
             title={showTitleOnHover ? '' : title}
             $sidebarIsPinned={sidebarIsPinned}
             $disabled={disabled}>
    <IconWrap $showTitleOnHover={showTitleOnHover}
              $isSelected={isSelected}
              $sidebarIsPinned={sidebarIsPinned}
              $disabled={disabled}>
      <Icon name={icon} />
    </IconWrap>
    {(showTitleOnHover && !isSelected) && <Title><span>{title}</span></Title>}
  </Container>
);

NavItem.propTypes = {
  icon: PropTypes.node.isRequired,
  isSelected: PropTypes.bool,
  showTitleOnHover: PropTypes.bool,
  sidebarIsPinned: PropTypes.bool.isRequired,
  title: PropTypes.string.isRequired,
  disabled: PropTypes.bool,
};

NavItem.defaultProps = {
  isSelected: false,
  showTitleOnHover: true,
  disabled: false,
};

export default NavItem;
