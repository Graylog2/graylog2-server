// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import PropTypes from 'prop-types';

type Props = {
  children: React.Node,
  delay: number,
};

const Delayed = ({ children, delay }: Props) => {
  const [delayFinished, setDelayFinished] = useState(delay <= 0);
  useEffect(() => {
    if (delay <= 0) {
      return () => {};
    }

    const delayTimeout = window.setTimeout(() => setDelayFinished(true), delay);

    return () => clearTimeout(delayTimeout);
  }, []);

  return delayFinished ? children : null;
};

Delayed.propTypes = {
  children: PropTypes.node.isRequired,
  delay: PropTypes.number.isRequired,
};

export default Delayed;
