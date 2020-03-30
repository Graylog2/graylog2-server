// @flow strict
import * as React from 'react';
import { useState, useEffect, useRef } from 'react';
import { isFunction } from 'lodash';
import isDeepEqual from './isDeepEqual';

type StoreType<State> = {
  getInitialState: () => State,
  listen: ((State) => mixed) => (() => void),
};

type ExtractStoreState = <V, Store: StoreType<V>>(Store) => V;
type ExtractComponentProps = <Props>(React.ComponentType<Props>) => Props;

type ResultType<Stores> = $ObjMap<Stores, ExtractStoreState>;

type PropsMapper<V, R> = (V) => R;

function id<V, R>(x: V) {
  // $FlowFixMe: Casting by force
  return (x: R);
}

export function useStore<V, Store: StoreType<V>, R>(store: Store, propsMapper: PropsMapper<V, R> = id): R {
  const [storeState, setStoreState] = useState(() => propsMapper(store.getInitialState()));
  const storeStateRef = useRef(storeState);
  useEffect(() => store.listen((newState) => {
    if (!isDeepEqual(newState, storeStateRef.current)) {
      setStoreState(propsMapper(newState));
      storeStateRef.current = newState;
    }
  }), [store]);
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

function connect<Stores: Object, Props, ComponentType: React.ComponentType<Props>, MappedProps>(
  Component: ComponentType,
  stores: Stores,
  mapProps: (ResultType<Stores>) => MappedProps = (props) => props,
): React.ComponentType<$Diff<$Call<ExtractComponentProps, ComponentType>, MappedProps>> {
  const wrappedComponentName = Component.displayName || Component.name || 'Unknown/Anonymous';
  class ConnectStoresWrapper extends React.Component<$Diff<$Call<ExtractComponentProps, ComponentType>, MappedProps>> {
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
      }).reduce((prev, [key, state]) => ({ ...prev, [key]: state }), {});

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
      return mapProps(storeProps);
    };

    render() {
      const nextProps = this._genProps(this.state);
      const { ref, ...componentProps } = this.props;

      return <Component {...nextProps} {...componentProps} />;
    }
  }
  ConnectStoresWrapper.displayName = `ConnectStoresWrapper[${wrappedComponentName}] stores=${Object.keys(stores).join(',')}`;

  return ConnectStoresWrapper;
}

export default connect;
