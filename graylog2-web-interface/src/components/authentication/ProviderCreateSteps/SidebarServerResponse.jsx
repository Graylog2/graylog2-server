// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

type Props = {
  children: string,
};

const SidebarConnectionCheck = ({ children }: Props) => (<>{children}</>);

SidebarConnectionCheck.propTypes = {
  children: PropTypes.string,
};

SidebarConnectionCheck.defaultProps = {
  children: 'Hello World!',
};

export default SidebarConnectionCheck;
