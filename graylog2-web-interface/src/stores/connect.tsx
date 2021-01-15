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
import * as React from 'react';
import { useState, useEffect, useRef } from 'react';
import { isFunction } from 'lodash';
import { Optional } from 'utility-types';

import isDeepEqual from './isDeepEqual';

type StoreType<State> = {
  getInitialState: () => State;
  listen: (cb: (state: State) => unknown) => () => void;
};

export type ExtractStoreState<Store> = Store extends StoreType<infer V> ? V : never;

export type ResultType<Stores> = { [K in keyof Stores]: ExtractStoreState<Stores[K]> };

type PropsWithDefaults<C extends React.ComponentType> = JSX.LibraryManagedAttributes<C, React.ComponentProps<C>>;

type PropsMapper<V, R> = (props: V) => R;

const id = <V, >(x: V): V => x;

export function useStore<U>(store: StoreType<U>): U;
export function useStore<U, M extends (props: U) => any>(store: StoreType<U>, propsMapper: M): ReturnType<M>;

export function useStore(store, propsMapper = id) {
  const [storeState, setStoreState] = useState(() => propsMapper(store.getInitialState()));
  const storeStateRef = useRef(storeState);

  useEffect(() => store.listen((newState) => {
    const mappedProps = propsMapper(newState);

    if (!isDeepEqual(mappedProps, storeStateRef.current)) {
      setStoreState(mappedProps);
      storeStateRef.current = mappedProps;
    }
  }), [propsMapper, store]);

  return storeState;
}

/**
 * Generating a higher order component wrapping an ES6 React component class, connecting it to the supplied stores.
 * The generated wrapper class passes the state it receives from the stores to the component it wraps as props.
 *
 * The `stores` parameter should consist of an object mapping desired props keys to stores. i.e.:
 * {
 *  samples: SamplesStore
 * }
 *
 * will connect to the provided `SamplesStore` and pass the state it receives from it to the supplied component as a
 * `samples` prop.
 *
 * @param {Object} Component - The component which should be connected to the stores.
 * @param {Object} stores - A mapping of desired props keys to stores.
 * @param {Function} mapProps - A function which is executed before props are passed to the component.
 * @returns {Object} - A React component wrapping the passed component.
 *
 * @example
 *
 * // Connecting the `SamplesComponent` class to the `SamplesStore`, hooking up its state to the `samples` prop and
 * // waiting for the store to settle down.
 *
 * connect(SamplesComponent, { samples: SamplesStore }, ({ samples }) => ({ samples: samples.filter(sample => sample.id === 4) }))
 *
 */

function connect<C extends React.ComponentType<React.ComponentProps<C>>, Stores extends object>(
  Component: C,
  stores: Stores
): React.ComponentType<Optional<PropsWithDefaults<C>, keyof Stores>>;

function connect<C extends React.ComponentType<React.ComponentProps<C>>, Stores extends object, MappedProps extends object>(
    Component: C,
    stores: Stores,
    mapProps: PropsMapper<ResultType<Stores>, MappedProps>
): React.ComponentType<Optional<PropsWithDefaults<C>, keyof MappedProps>>;

function connect<
    C extends React.ComponentType<React.ComponentProps<C>>,
    Stores,
    MappedProps extends object,
    >(
  Component: C,
  stores: Stores,
  mapProps: PropsMapper<ResultType<Stores>, MappedProps> = (props: ResultType<Stores>) => props as MappedProps,
): React.ComponentType<Optional<PropsWithDefaults<C>, keyof MappedProps>> {
  const wrappedComponentName = Component.displayName || Component.name || 'Unknown/Anonymous';

  class ConnectStoresWrapper extends React.Component<Optional<PropsWithDefaults<C>, keyof MappedProps>> {
    // eslint-disable-next-line react/state-in-constructor
    state: ResultType<Stores>;

    unsubscribes: Array<() => void>;

    constructor(props) {
      super(props);

      // Retrieving initial state from each configured store
      const storeStates = Object.keys(stores).map((key) => {
        const store = stores[key];

        if (store === undefined || !isFunction(store.getInitialState)) {
          // eslint-disable-next-line no-console
          console.error(`Error: The store passed for the \`${key}\` property is not defined or invalid. Check the connect()-call wrapping your \`${wrappedComponentName}\` component.`);

          return [key, undefined];
        }

        const state = store.getInitialState();

        return [key, state];
      }).reduce((prev, [key, state]) => ({ ...prev, [key as string]: state }), {});

      this.state = { ...this.state, ...storeStates };
    }

    componentDidMount() {
      this.unsubscribes = Object.keys(stores).map((key) => {
        const store = stores[key];

        if (store === undefined || !isFunction(store.listen)) {
          // eslint-disable-next-line no-console
          console.error(`Error: The store passed for the \`${key}\` property is not defined or invalid. Check the connect()-call wrapping your \`${wrappedComponentName}\` component.`);

          return () => {};
        }

        return store.listen((partialState) => this.setState((state) => ({ ...state, [key]: partialState })));
      });
    }

    shouldComponentUpdate(nextProps, nextState) {
      const thisChildProps = this._genProps(this.state);
      const nextChildProps = this._genProps(nextState);

      return !(isDeepEqual(thisChildProps, nextChildProps) && isDeepEqual(this.props, nextProps));
    }

    componentWillUnmount() {
      this.unsubscribes.forEach((unsub) => unsub());
    }

    _genProps = (state: ResultType<Stores>): MappedProps => {
      const storeProps = {};

      Object.keys(stores).forEach((key) => {
        storeProps[key] = state[key];
      });

      return mapProps(storeProps as ResultType<Stores>);
    };

    render() {
      const nextProps = this._genProps(this.state);
      // @ts-ignore
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { ref, ...componentProps } = this.props;

      return <Component {...nextProps as MappedProps} {...componentProps as PropsWithDefaults<C>} />;
    }
  }

  // @ts-ignore
  ConnectStoresWrapper.displayName = `ConnectStoresWrapper[${wrappedComponentName}] stores=${Object.keys(stores).join(',')}`;

  return ConnectStoresWrapper;
}

export default connect;
