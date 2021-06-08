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
import CancellablePromise from 'logic/rest/CancellablePromise';

describe('CancellablePromise', () => {
  it('should cancel a promise', () => {
    const promise = CancellablePromise.of(Promise.resolve(() => true)).cancel();

    expect(promise.isCancelled()).toBeTruthy();
  });

  it('should implement then', async () => {
    return CancellablePromise.of(Promise.resolve('ハチ公')).then((result) => {
      expect(result).toBe('ハチ公');
    });
  });

  it('should implement finally', () => {
    CancellablePromise.of(Promise.resolve('ハチ公')).finally(() => {
      expect(true).toBeTruthy();
    });

    return Promise.resolve(() => {
      expect.assertions(1);
    });
  });

  it('should implement catch', () => {
    return CancellablePromise.of(Promise.resolve(() => {
      throw new Error('ハチ公');
    })).catch((error) => {
      expect(error).toBe('ハチ公');
    });
  });
});
