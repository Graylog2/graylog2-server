// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Alert } from 'components/graylog';

type Props = {
  children: React.Node,
  className: ?string,
};

const EmptyResult = ({ children, className }: Props) => (
  <Alert className={`${className ?? ''} no-bm`}>{children}</Alert>
);

EmptyResult.propTypes = {
  children: PropTypes.element,
  className: PropTypes.string,
};

EmptyResult.defaultProps = {
  children: 'No data available.',
  className: undefined,
};

export default EmptyResult;
