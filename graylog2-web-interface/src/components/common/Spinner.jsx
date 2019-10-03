import React from 'react';
import PropTypes from 'prop-types';

import Icon from './Icon';

/**
 * Simple spinner to use while waiting for something to load.
 */
const Spinner = ({ name, text, ...props }) => <span><Icon name={name} spin {...props} /> {text}</span>;

Spinner.propTypes = {
  /** Name of the Icon to use. */
  name: PropTypes.string,
  /** Text to show while loading. */
  text: PropTypes.string,
};
Spinner.defaultProps = {
  name: 'spinner',
  text: 'Loading...',
};

export default Spinner;
