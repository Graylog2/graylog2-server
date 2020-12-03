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
declare global {
  interface Window {
    singletons: { [key: string]: unknown };
  }
}

// eslint-disable-next-line arrow-parens
const singleton = <R>(key: string, supplier: () => R): R => {
  if (!window.singletons[key]) {
    window.singletons[key] = supplier();
  }

  return window.singletons[key] as R;
};

const singletonActions = <R>(key: string, supplier: () => R): R => singleton(`${key}Actions`, supplier);

const singletonStore = <R>(key: string, supplier: () => R): R => singleton(`${key}Store`, supplier);

if (typeof window.singletons === 'undefined') {
  window.singletons = {};
}

export { singleton, singletonActions, singletonStore };
