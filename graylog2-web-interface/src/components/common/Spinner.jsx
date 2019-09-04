import React from 'react';
import PropTypes from 'prop-types';
import { Icon } from 'components/graylog';

/**
 * Simple spinner to use while waiting for something to load.
 */
const Spinner = ({ text }) => <span><Icon className="fa fa-spin fa-spinner" /> {text}</span>;

Spinner.propTypes = {
  /** Text to show while loading. */
  text: PropTypes.string,
};
Spinner.defaultProps = {
  text: 'Loading...',
};

export default Spinner;
