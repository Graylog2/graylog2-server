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
class CancellablePromise<T> implements Promise<T> {
  private _isCanceled: boolean;

  private _promise: Promise<T>;

  private constructor(promise: Promise<T>, isCancelled = false) {
    this._promise = promise;
    this._isCanceled = isCancelled;
  }

  [Symbol.toStringTag]: string;

  then<TResult1 = T, TResult2 = never>(onfulfilled?: (value: T) => TResult1 | PromiseLike<TResult1>, onrejected?: (reason: any) => TResult2 | PromiseLike<TResult2>) {
    return new CancellablePromise(this._promise.then(onfulfilled, onrejected), this._isCanceled);
  }

  catch<TResult = never>(onrejected?: (reason: any) => TResult | PromiseLike<TResult>) {
    return new CancellablePromise(this._promise.catch(onrejected), this._isCanceled);
  }

  finally(onfinally?: () => void) {
    return new CancellablePromise(this._promise.finally(onfinally), this._isCanceled);
  }

  static of<R>(promise: Promise<R>) {
    return new CancellablePromise<R>(promise);
  }

  public cancel() {
    this._isCanceled = true;

    return this;
  }

  public isCancelled() {
    return this._isCanceled;
  }
}

export default CancellablePromise;
