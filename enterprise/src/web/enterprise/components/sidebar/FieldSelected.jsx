import React from 'react';
import PropTypes from 'prop-types';

const FieldSelected = ({ name, selected, onToggleSelected }) => {
  const onChange = () => onToggleSelected(name);
  return (
    <input type="checkbox" onChange={onChange} checked={selected} />
  );
};

FieldSelected.propTypes = {
  name: PropTypes.string.isRequired,
  selected: PropTypes.bool.isRequired,
  onToggleSelected: PropTypes.func.isRequired,
};

export default FieldSelected;
