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
declare module 'reflux' {
  import { RefluxActions, Store } from 'stores/StoreTypes';

  export interface StoreDefinition {
    listenables?: any[];
    init?: Function;
    getInitialState?: Function;

    [propertyName: string]: any;
  }

  export interface ActionsDefinition {
    [index: string]: { asyncResult: boolean};
  }

  type ElementType <T extends ReadonlyArray<unknown>> = T extends ReadonlyArray<infer E> ? E : never;

  export function createStore<T>(definition: StoreDefinition): Store<T> & typeof definition;
  export function createActions<R>(definitions: ActionsDefinition): RefluxActions<R>;
  export function createActions<R>(definitions: R): RefluxActions<{ [key in ElementType<typeof definitions>]: () => Promise<unknown> }>;
  export function connect(store: Store, key?: string): void;
}
