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

import set from 'lodash/set';
import type { FormikErrors } from 'formik';

import FetchError from 'logic/errors/FetchError';

import type { TunableValues } from './types';

export const GLOBAL_ERROR_KEY = '_global';

type ValidationBody = { errors?: Record<string, Array<string>> };

export const toFormErrors = (error: unknown) => {
  if (!(error instanceof FetchError) || error.status !== 400) {
    return undefined;
  }
  const body = error.additional?.body as ValidationBody | undefined;
  if (!body?.errors) {
    return undefined;
  }
  const fieldErrors: FormikErrors<TunableValues> = {};
  const globalErrors: Array<string> = [];
  Object.entries(body.errors).forEach(([path, messages]) => {
    if (path === GLOBAL_ERROR_KEY) {
      globalErrors.push(...messages);
    } else {
      set(fieldErrors, path, messages.join(', '));
    }
  });

  return { fieldErrors, globalErrors };
};
