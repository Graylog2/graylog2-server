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

import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';

export const validationErrorExplanation = {
  id: 'validation-explanation-id',
  errorType: 'QUERY_PARSING_ERROR',
  errorTitle: 'Parse Exception',
  errorMessage: "Cannot parse 'source: '",
  beginLine: 1,
  endLine: 1,
  beginColumn: 1,
  endColumn: 5,
};
// eslint-disable-next-line import/prefer-default-export
export const validationError: QueryValidationState = {
  status: 'ERROR',
  explanations: [validationErrorExplanation],
};
