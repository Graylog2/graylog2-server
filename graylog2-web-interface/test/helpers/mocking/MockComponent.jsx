import React from 'react';
import PropTypes from 'prop-types';
import { kebabCase } from 'lodash';

export default (name) => {
  const MockComponent = ({ children, ...rest }) => React.createElement(kebabCase(name), rest, children);

  MockComponent.propTypes = {
    children: PropTypes.node,
  };
  MockComponent.defaultProps = {
    children: null,
  };
  MockComponent.displayName = name;

  return MockComponent;
};
