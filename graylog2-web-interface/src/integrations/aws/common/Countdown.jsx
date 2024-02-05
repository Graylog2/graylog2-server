/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
