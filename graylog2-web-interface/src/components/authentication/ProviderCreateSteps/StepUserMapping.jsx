// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

type Props = {
  children: string,
};

const StepUserMapping = ({ children }: Props) => (<>{children}</>);

StepUserMapping.propTypes = {
  children: PropTypes.string,
};

StepUserMapping.defaultProps = {
  children: 'Hello World!',
};

export default StepUserMapping;
