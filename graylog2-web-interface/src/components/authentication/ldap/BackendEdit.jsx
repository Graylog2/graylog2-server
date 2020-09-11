// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

type Props = {
  children: string,
};

const BackendEdit = ({ children }: Props) => (<>{children}</>);

BackendEdit.propTypes = {
  children: PropTypes.string,
};

BackendEdit.defaultProps = {
  children: 'Hello World!',
};

export default BackendEdit;
