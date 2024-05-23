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
import debounceWithPromise from './debounceWithPromise';

describe('debounceWithPromise', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });

  it('never returns a pending promise', async () => {
    const fn = jest.fn(async (attempt: number) => attempt);

    const debouncedFn = debounceWithPromise(fn, 300);

    const result1 = debouncedFn(1);
    const result2 = debouncedFn(2);
    const result3 = debouncedFn(3);

    jest.advanceTimersByTime(400);

    await expect(result1).resolves.toBe(3);
    await expect(result2).resolves.toBe(3);
    await expect(result3).resolves.toBe(3);
  });
});
