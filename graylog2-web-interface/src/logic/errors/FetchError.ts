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
import { isString } from 'lodash';

type Res = {
  text?: string;
}

type Additional = {
  status: number;
  body?: any;
  res?: Res;
}

export default class FetchError extends Error {
  name: 'FetchError';

  responseMessage: string;

  additional: Additional;

  status: Additional['status'];

  constructor(message: string, status: number, additional: any) {
    super(message);
    this.name = 'FetchError';

    const details = isString(additional) ? additional : (additional?.message ?? 'Not available');
    this.message = `There was an error fetching a resource: ${message}. Additional information: ${details}`;

    this.responseMessage = additional?.message ?? undefined;

    this.additional = { body: additional, status, res: { text: isString(additional) && additional } };
    this.status = status; // Shortcut, as this is often used
  }
}
