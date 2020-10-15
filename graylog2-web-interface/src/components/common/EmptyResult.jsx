// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Alert } from 'components/graylog';

type Props = {
  children: React.Node,
};

const EmptyResult = ({ children }: Props) => (<Alert>{children}</Alert>);

EmptyResult.propTypes = {
  children: PropTypes.element,
};

EmptyResult.defaultProps = {
  children: 'No data available!',
};

export default EmptyResult;
