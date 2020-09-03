// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

type Props = {
  children: string,
};

const StepServerConfiguration = ({ children }: Props) => (<>{children}</>);

StepServerConfiguration.propTypes = {
  children: PropTypes.string,
};

StepServerConfiguration.defaultProps = {
  children: 'Hello World!',
};

export default StepServerConfiguration;
