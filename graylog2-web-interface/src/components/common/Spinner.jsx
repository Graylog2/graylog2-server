// @flow strict
import React, { useState, useEffect } from 'react';
import type { ComponentType } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import Icon from './Icon';

const Wrapper: ComponentType<{ visible: boolean }> = styled.span`
  visibility: ${({ visible }) => (visible ? 'visible' : 'hidden')};
`;

type Props = {
  delay?: number,
  name?: string,
  text?: string,
}

/**
 * Simple spinner to use while waiting for something to load.
 */
const Spinner = ({ name, text, delay, ...rest }: Props) => {
  const [delayFinished, setDelayFinished] = useState(false);

  useEffect(() => {
    const delayTimeout = window.setTimeout(() => {
      setDelayFinished(true);
    }, delay);

    return () => clearTimeout(delayTimeout);
  }, []);

  return (
    <Wrapper visible={delayFinished}>
      <Icon name={name} spin {...rest} /> {text}
    </Wrapper>
  );
};


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
