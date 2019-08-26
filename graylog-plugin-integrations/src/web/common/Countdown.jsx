import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';

function Countdown({ callback, className, timeInSeconds, paused }) {
  const [currentTime, setCurrentTime] = useState('00:00');
  let logInterval;
  let duration = timeInSeconds;

  const startCountdown = () => {
    let minutes;
    let seconds;

    logInterval = setInterval(() => {
      minutes = parseInt(duration / 60, 10);
      seconds = parseInt(duration % 60, 10);

      minutes = minutes < 10 ? `0${minutes}` : minutes;
      seconds = seconds < 10 ? `0${seconds}` : seconds;

      duration -= 1;

      if (duration < 0) {
        duration = timeInSeconds;
        setCurrentTime('00:00');
        clearInterval(logInterval);
        callback();
      } else {
        setCurrentTime(`${minutes}:${seconds}`);
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
