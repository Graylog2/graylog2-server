import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { duration } from 'moment';

function Countdown({ callback, className, timeInSeconds, paused }) {
  let tickTock = timeInSeconds;
  let logInterval;

  const defaultDuration = duration(timeInSeconds, 'seconds').format('mm:ss');
  const [currentTime, setCurrentTime] = useState(defaultDuration);

  const startCountdown = () => {
    logInterval = setInterval(() => {
      tickTock -= 1;

      const currentDuration = duration(tickTock, 'seconds').format('mm:ss', { trim: false });

      if (tickTock < 0) {
        tickTock = timeInSeconds;
        setCurrentTime(defaultDuration);
        clearInterval(logInterval);
        callback();
      } else {
        setCurrentTime(currentDuration);
      }
    }, 1000);
  };

  useEffect(() => {
    if (paused) {
      clearInterval(logInterval);
    } else {
      startCountdown();
    }

    return () => {
      clearInterval(logInterval);
    };
  }, [paused]);

  return (
    <span className={className}>{currentTime}</span>
  );
}

Countdown.propTypes = {
  timeInSeconds: PropTypes.number.isRequired,
  callback: PropTypes.func,
  className: PropTypes.string,
  paused: PropTypes.bool,
};

Countdown.defaultProps = {
  callback: () => {},
  className: '',
  paused: false,
};

export default Countdown;
