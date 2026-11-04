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
export type BeatsInputDefaultsConfigType = {
  max_event_size: number;
};

// Keep in sync with BeatsInputDefaultsConfig.DEFAULT_MAX_EVENT_SIZE
export const DEFAULT_BEATS_INPUT_DEFAULTS_CONFIG: BeatsInputDefaultsConfigType = {
  max_event_size: 16 * 1024 * 1024,
};

// Frame sizes are 32-bit integers in the Beats protocol, the backend stores the limit as a Java int.
const MAX_INT = 2 ** 31 - 1;

export const validateBeatsInputDefaults = ({ max_event_size }: BeatsInputDefaultsConfigType) => {
  if (!Number.isInteger(max_event_size) || max_event_size < 1 || max_event_size > MAX_INT) {
    return { max_event_size: `Must be a whole number between 1 and ${MAX_INT} bytes.` };
  }

  return {};
};
