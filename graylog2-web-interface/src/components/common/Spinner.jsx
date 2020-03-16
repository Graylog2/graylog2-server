// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import Icon from './Icon';
import Delayed from './Delayed';

type Props = {
  delay: number,
  name?: string,
  text?: string,
}

/**
 * Simple spinner to use while waiting for something to load.
 */
const Spinner = ({ name, text, delay, ...rest }: Props) => (
  <Delayed delay={delay}>
    <Icon {...rest} name={name} spin /> {text}
  </Delayed>
);

Spinner.propTypes = {
  /** Delay in ms before displaying the spinner */
  delay: PropTypes.number,
  /** Name of the Icon to use. */
  name: PropTypes.string,
  /** Text to show while loading. */
  text: PropTypes.string,
};

Spinner.defaultProps = {
  name: 'spinner',
  text: 'Loading...',
  delay: 200,
};

export default Spinner;
