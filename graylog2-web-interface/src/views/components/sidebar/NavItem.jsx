// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { SizeMe } from 'react-sizeme';
import styled, { css } from 'styled-components';

import { Icon } from 'components/common';
import CustomPropTypes from '../CustomPropTypes';

type StyleProps = {
  isSelected: boolean,
  expandRight: boolean,
};

export const Title: React.ComponentType<StyleProps> = styled.div`
  padding: 9px 10px;
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;

  ${props => (props.isSelected ? css`
    color: #FF3633;
    background: #393939;
  ` : '')}

  ${props => (props.isSelected && props.expandRight ? css`
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
  ` : '')}
`;

const TitleText = styled.div`
  font-size: 16px;
  display: inline;
  margin-left: 10px;
  overflow: hidden;
  white-space: nowrap;
`;

const TitleIcon = styled.div`
  width: 25px;
  text-align: center;
  font-size: 20px;
  cursor: pointer;
`;

const Content: React.ComponentType<StyleProps> = styled.div`
  color: #666666;
  background: #FFFFFF;
  box-shadow:
          inset 0px 13px 5px -10px #CCC,
          inset 0px -13px 5px -10px #CCC;

  ${props => (props.isSelected ? css`
    padding: 20px;
  ` : css`
    max-height: 0;
  `)}

  ${props => (props.isSelected && props.expandRight ? css`
    position: absolute !important;
    top: 0;
    left: 100%;
    border: 0;
    bottom: 0;
    padding: 20px;
    width: 300px;
    overflow-y: hidden;
  ` : '')}
`;

type Props = {
  isOpen: boolean,
  isSelected: boolean,
  expandRight: boolean,
  text: string,
  icon: string,
  onClick: (string) => void,
  children: React.Element<any>,
}

const NavItem = ({ isOpen, isSelected, expandRight, text, children, icon, onClick }: Props) => {
  // eslint-disable-next-line no-nested-ternary
  return (
    <React.Fragment>
      <Title role="presentation" onClick={onClick} isSelected={isSelected} expandRight={expandRight}>
        <TitleIcon><Icon name={icon} /></TitleIcon>
        {(isOpen && <TitleText>{text}</TitleText>)}
      </Title>
      <SizeMe monitorHeight refreshRate={100}>
        {({ size }) => {
          return (
            <Content isSelected={isSelected} expandRight={expandRight}>
              {
                isSelected && children
                  ? React.cloneElement(children, { listHeight: size.height - 150 })
                  : <span />
              }
            </Content>
          );
        }}
      </SizeMe>
    </React.Fragment>
  );
};

NavItem.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  isSelected: PropTypes.bool.isRequired,
  expandRight: PropTypes.bool,
  text: PropTypes.string.isRequired,
  icon: PropTypes.node.isRequired,
  children: CustomPropTypes.OneOrMoreChildren.isRequired,
};

NavItem.defaultProps = {
  expandRight: false,
};

export default NavItem;
