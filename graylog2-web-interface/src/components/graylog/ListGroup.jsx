import React from 'react';
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import { ListGroup as BootstrapListGroup } from 'react-bootstrap';

const ListGroup = ({ className, ...props }) => {
  return <BootstrapListGroup bsClass={className} {...props} />;
};

ListGroup.propTypes = {
  className: PropTypes.string,
};
ListGroup.defaultProps = {
  className: undefined,
};

/** @component */
export default ListGroup;
