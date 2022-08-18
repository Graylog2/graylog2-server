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
import { debounce } from 'lodash';

type PromiseReturnType<T> = T extends (...args: any[]) => Promise<infer R> ? R : never;

const debounceWithPromise = <T extends (...args: any[]) => Promise<any>>(fn: T, delay: number) => {
  const debouncedFn = debounce((resolve: PromiseReturnType<T>, ...args: Parameters<T>) => fn(...args).then(resolve), delay);

  return (...args: Parameters<T>) => new Promise<PromiseReturnType<T>>((resolve: PromiseReturnType<T>) => {
    debouncedFn(resolve, ...args);
  });
};

export default debounceWithPromise;
