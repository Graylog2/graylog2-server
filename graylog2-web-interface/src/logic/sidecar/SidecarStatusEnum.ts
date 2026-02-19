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
import lowerCase from 'lodash/lowerCase';
import toNumber from 'lodash/toNumber';

type StatusCode = 0 | 1 | 2 | 3;

type StatusProperty = {
  name: string;
};

const SidecarStatusEnum: {
  RUNNING: StatusCode;
  UNKNOWN: StatusCode;
  FAILING: StatusCode;
  STOPPED: StatusCode;
  properties: Record<StatusCode, StatusProperty>;
  isValidStatusCode: (statusCode: number) => boolean;
  toStatusCode: (stringStatus: string) => StatusCode;
  toString: (statusCode: number | string) => string;
} = {
  RUNNING: 0,
  UNKNOWN: 1,
  FAILING: 2,
  STOPPED: 3,
  properties: {
    0: { name: 'running' },
    1: { name: 'unknown' },
    2: { name: 'failing' },
    3: { name: 'stopped' },
  },

  isValidStatusCode(statusCode: number): boolean {
    return Object.keys(this.properties).includes(String(statusCode));
  },

  toStatusCode(stringStatus: string): StatusCode {
    const status = lowerCase(stringStatus);

    if (status === this.properties[this.RUNNING].name) {
      return this.RUNNING;
    }

    if (status === this.properties[this.FAILING].name) {
      return this.FAILING;
    }

    if (status === this.properties[this.STOPPED].name) {
      return this.STOPPED;
    }

    return this.UNKNOWN;
  },

  toString(statusCode: number | string): string {
    switch (toNumber(statusCode)) {
      case this.RUNNING:
        return 'running';
      case this.FAILING:
        return 'failing';
      case this.STOPPED:
        return 'stopped';
      default:
        return 'unknown';
    }
  },
};

export default SidecarStatusEnum;
