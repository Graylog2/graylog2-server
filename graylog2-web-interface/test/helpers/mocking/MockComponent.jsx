import React from 'react';
import PropTypes from 'prop-types';

export default (name) => {
  const MockComponent = props => (
    <span className={name}>
      <span {...props}>{props.children}</span>
    </span>
  );
  MockComponent.propTypes = {
    children: PropTypes.element,
  };
  MockComponent.defaultProps = {
    children: null,
  };

  return MockComponent;
};
