// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { SizeMe } from 'react-sizeme';

import { Icon } from 'components/common';
import { Title, TitleText, TitleIcon, Content } from './NavItem.styles';
import CustomPropTypes from '../CustomPropTypes';

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
