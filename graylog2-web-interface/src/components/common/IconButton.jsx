// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import Icon from 'components/common/Icon';

const Wrapper: StyledComponent<{}, ThemeInterface, HTMLButtonElement> = styled.button(({ theme }) => `
  display: inline-flex;
  justify-content: center;
  align-items: center;
  
  height: 25px;
  width: 25px;
  border: 0

  cursor: pointer;
  color: ${theme.color.gray[70]};
  font-size: 16px;

  :hover {
    background-color: ${theme.color.gray[90]};
  }

  :active {
    background-color: ${theme.color.gray[80]};
  }
`);


type Props = {
  title: string,
  onClick?: () => void,
};

const handleClick = (onClick) => {
  if (typeof onClick === 'function') {
    onClick();
  }
};

const IconButton = ({ title, onClick, ...rest }: Props) => (
  <Wrapper title={title} onClick={() => handleClick(onClick)}>
    <Icon {...rest} />
  </Wrapper>
);

IconButton.propTypes = {
  title: PropTypes.string,
  onClick: PropTypes.func,
};

IconButton.defaultProps = {
  onClick: undefined,
  title: undefined,
};

export default IconButton;
