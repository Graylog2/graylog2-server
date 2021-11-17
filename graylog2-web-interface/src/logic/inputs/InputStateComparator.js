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
class InputStateComparator {
  constructor() {
    this.mapping = {
      CREATED: 0,
      INITIALIZED: 1,
      INVALID_CONFIGURATION: 2,
      STARTING: 3,
      RUNNING: 4,
      FAILED: 2,
      STOPPING: 1,
      STOPPED: 0,
      TERMINATED: 0,
    };
  }

  compare(state1, state2) {
    return this.mapping[state1.toUpperCase()] - this.mapping[state2.toUpperCase()];
  }
}

export default InputStateComparator;
