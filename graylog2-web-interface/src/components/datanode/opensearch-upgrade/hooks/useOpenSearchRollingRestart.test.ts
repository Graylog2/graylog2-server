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
import FetchError from 'logic/errors/FetchError';

import { rollingRestartStartError } from './useOpenSearchRollingRestart';

const fetchErrorWithBody = (status: number, body: unknown) => new FetchError('Bad Request', status, body);

describe('rollingRestartStartError', () => {
  it('offers a force retry and strips the force hint when every check is overridable', () => {
    const failedChecks = ['Cluster status is YELLOW — must be GREEN (pass force=true to override)'];
    const result = rollingRestartStartError(fetchErrorWithBody(400, { failed_checks: failedChecks }));

    expect(result.canRetryWithForce).toBe(true);
    expect(result.failedChecks).toEqual(['Cluster status is YELLOW — must be GREEN']);
    expect(result.message).toBe('Cluster status is YELLOW — must be GREEN');
  });

  it('does not offer a force retry for non-overridable precondition failures', () => {
    const failedChecks = ['Need at least 3 DataNodes for safe rolling restart (found 2)'];
    const result = rollingRestartStartError(fetchErrorWithBody(400, { failed_checks: failedChecks }));

    expect(result.canRetryWithForce).toBe(false);
    expect(result.failedChecks).toEqual(failedChecks);
  });

  it('does not offer a force retry when only some checks are overridable', () => {
    const result = rollingRestartStartError(
      fetchErrorWithBody(400, {
        failed_checks: [
          'Cluster status is YELLOW — must be GREEN (pass force=true to override)',
          'Need at least 3 DataNodes for safe rolling restart (found 2)',
        ],
      }),
    );

    expect(result.canRetryWithForce).toBe(false);
  });

  it('uses the error body message when there are no failed checks', () => {
    const result = rollingRestartStartError(fetchErrorWithBody(409, { error: 'Another rolling restart job is already active' }));

    expect(result.canRetryWithForce).toBe(false);
    expect(result.failedChecks).toEqual([]);
    expect(result.message).toBe('Another rolling restart job is already active');
  });

  it('falls back to a readable message for non-FetchError values', () => {
    const result = rollingRestartStartError('boom');

    expect(result.canRetryWithForce).toBe(false);
    expect(result.message).toBe('boom');
  });
});
