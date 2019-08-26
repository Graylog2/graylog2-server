import React from 'react';
import PropTypes from 'prop-types';

function Options({ value, label }) {
  return (
    <option value={value} key={value}>{label}</option>
  );
}

const renderOptions = (options = [], label = 'Choose One', loading = false) => {
  if (loading) {
    return Options({ value: '', label: 'Loading...' });
  }

  return (
    <>
      <option value="">{label}</option>
      {options.map(option => Options({ value: option.value, label: option.label }))}
    </>
  );
};

Options.propTypes = {
  value: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
};

export default Options;

export { renderOptions };
