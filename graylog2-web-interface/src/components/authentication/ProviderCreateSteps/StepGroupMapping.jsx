// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

type Props = {
  children: string,
};

const StepGroupMapping = ({ children }: Props) => (<>{children}</>);

StepGroupMapping.propTypes = {
  children: PropTypes.string,
};

StepGroupMapping.defaultProps = {
  children: 'Hello World!',
};

export default StepGroupMapping;
