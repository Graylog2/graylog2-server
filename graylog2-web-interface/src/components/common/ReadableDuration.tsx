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

import * as React from 'react';

import { durationInSeconds, durationInMinutes } from 'util/DateTime';

import Pluralize from './Pluralize';

type Props = {
  duration: number | string
}

/**
 * Takes a duration (e.g. in milliseconds or seconds, or as a ISO8601 duration) and displays it in a readable format.
 */

const ReadableDuration = ({ duration }: Props) => {
  const durationInSec = durationInSeconds(duration);
  const durationInMin = durationInMinutes(duration);

  if (durationInSec < 60) {
    return (
      <span>{durationInSec} <Pluralize singular="second"
                                       plural="seconds"
                                       value={durationInSec} />
      </span>
    );
  }

  return (
    <span>{durationInMin} <Pluralize singular="minute"
                                     plural="minutes"
                                     value={durationInMin} />
    </span>
  );
};

export default ReadableDuration;
