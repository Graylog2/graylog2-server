import React from 'react';
import PropTypes from 'prop-types';
import { Button } from 'react-bootstrap';

const EditModeToggleButton = ({ value, onToggle }) => {
  return (
    <span className="pull-right" style={{ position: 'relative', zIndex: 1 }}>
      <Button bsStyle="success" bsSize="small" onClick={onToggle}>{value ? 'Done' : 'Edit'}</Button>
    </span>
  );
};

EditModeToggleButton.propTypes = {
  onToggle: PropTypes.func.isRequired,
  value: PropTypes.bool,
};

EditModeToggleButton.defaultProps = {
  value: false,
};

export default EditModeToggleButton;
