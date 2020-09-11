// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

type Props = {
  children: string,
};

const ServieEdit = ({ children }: Props) => (<>{children}</>);

ServieEdit.propTypes = {
  children: PropTypes.string,
};

ServieEdit.defaultProps = {
  children: 'Hello World!',
};

export default ServieEdit;
