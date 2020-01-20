// @flow strict
import * as React from 'react';
import styled, { css } from 'styled-components';

type StyleProps = {
  isSelected: boolean,
  expandRight: boolean,
};

export const Title: React.ComponentType<StyleProps> = styled.div(({ isSelected, expandRight }) => css`
  padding: 9px 10px;
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
  color: ${isSelected ? '#FF3633' : 'inherit'};
  background: ${isSelected ? '#393939' : 'tranparent'};
  
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

export const TitleText: React.ComponentType<{}> = styled.div`
  font-size: 16px;
  display: inline;
  margin-left: 10px;
  overflow: hidden;
  white-space: nowrap;
`;

export const TitleIcon: React.ComponentType<{}> = styled.div`
  width: 25px;
  text-align: center;
  font-size: 20px;
  cursor: pointer;
`;

export const Content: React.ComponentType<StyleProps> = styled.div(({ isSelected, expandRight }) => css`
  color: #666666;
  background: #FFFFFF;
  box-shadow:
      inset 0px 13px 5px -10px #CCC,
      inset 0px -13px 5px -10px #CCC;
  
  ${(isSelected ? css`
    padding: 20px;
  ` : css`
    max-height: 0;
  `)}
  
  ${(isSelected && expandRight) && css`
    position: absolute !important;
    top: 0;
    left: 100%;
    border: 0;
    bottom: 0;
    padding: 20px;
    width: 450px;
    overflow-y: hidden;
  `}
`);
