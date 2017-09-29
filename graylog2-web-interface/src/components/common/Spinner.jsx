import React from 'react';
import PropTypes from 'prop-types';

const Spinner = ({ text }) => <span><i className="fa fa-spin fa-spinner" /> {text}</span>;

Spinner.propTypes = {
  text: PropTypes.string,
};
Spinner.defaultProps = {
  text: 'Loading...',
};

export default Spinner;
