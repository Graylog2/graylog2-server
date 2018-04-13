import React from 'react';
import PropTypes from 'prop-types';

import GraylogLogo from 'components/common/GraylogLogo';

/**
 * Simple spinner to use while waiting for something to load.
 */
const Spinner = ({ text, logo }) => {
  const content = logo ? <GraylogLogo text={text} animated /> : text;
  return (
    <span>{content}</span>
  );
};

Spinner.propTypes = {
  /** Text to show while loading. */
  text: PropTypes.string,
  /** Show fullscreen logo as spinner. */
  logo: PropTypes.bool,
};

Spinner.defaultProps = {
  text: 'Loading...',
  logo: false,
};

export default Spinner;
