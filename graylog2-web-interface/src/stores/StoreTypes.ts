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
export type PromiseProvider = (...args: any[]) => Promise<any>;
type ExtractResultType<R extends PromiseProvider> = ExtractTypeFromPromise<ReturnType<R>>;
type ExtractTypeFromPromise<P> = P extends Promise<infer R> ? R : P;
type SyncAction = (...args: any[]) => void;

export type ListenableAction<R extends PromiseProvider> = R & {
  triggerPromise: R;
  listen: (cb: (result: ExtractResultType<R>) => any) => () => void;
  completed: {
    (result: ExtractResultType<R>): void;
    listen: (cb: (result: ExtractResultType<R>) => any) => () => void;
  };
  promise: (promise: ReturnType<R>) => void;
};

export type SyncListenableAction<R extends SyncAction> = R & {
  listen: (cb: () => any) => () => void;
};

export type RefluxActions<A extends { [key: string]: PromiseProvider }> = { [P in keyof A]: ListenableAction<A[P]> };
export type SyncRefluxActions<A extends { [key: string]: SyncAction }> = { [P in keyof A]: SyncListenableAction<A[P]> };

export type Store<State> = {
  listen: (cb: (state: State) => unknown) => () => void;
  getInitialState: () => State;
};

export type StoreState<R> = R extends Store<infer T> ? T : never;
