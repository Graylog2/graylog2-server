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

type Body = {
  message: string;
  type: string;
}

type Additional = {
  status: number;
  body: Body;
};

export default class FetchError extends Error {
  responseMessage: string;

  additional: Additional;

  status: Additional['status'];

  constructor(message, additional) {
    super(message);
    this.message = message ?? additional?.message ?? 'Undefined error.';

    /* eslint-disable no-console */
    try {
      this.responseMessage = additional.body ? additional.body.message : undefined;

      console.error(`There was an error fetching a resource: ${this.message}.`,
        `Additional information: ${additional.body && additional.body.message ? additional.body.message : 'Not available'}`);
    } catch (e) {
      console.error(`There was an error fetching a resource: ${this.message}. No additional information available.`);
    }
    /* eslint-enable no-console */

    this.additional = additional;
    this.status = additional?.status; // Shortcut, as this is often used
  }
}
