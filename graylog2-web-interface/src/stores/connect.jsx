import React from 'react';
import { ListenerMethods } from 'reflux';
import { isFunction } from 'lodash';
import * as Immutable from 'immutable';
import isDeepEqual from './isDeepEqual';

const _isEqual = (first, second) => {
  if (first && first.equals && isFunction(first.equals)) {
    return Immutable.is(first, second);
  }
  if (Immutable.is(first, second)) {
    return true;
  }
  return undefined;
};

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

export default (Component, stores, mapProps = props => props) => {
  const wrappedComponentName = Component.displayName || Component.name || 'Unknown/Anonymous';
  class ConnectStoresWrapper extends React.Component {
    constructor(props) {
      super(props);

      if (!this.state) {
        this.state = {};
      }

      Object.keys(ListenerMethods).forEach((listenerMethod) => {
        this[listenerMethod] = ListenerMethods[listenerMethod].bind(this);
      });
      this.componentWillUnmount = ListenerMethods.stopListeningToAll.bind(this);

      // Retrieving initial state from each configured store
      Object.keys(stores).forEach((key) => {
        const store = stores[key];
        if (store === undefined) {
          // eslint-disable-next-line no-console
          console.error(`Error: The store passed for the \`${key}\` property is not defined. Check the connect()-call wrapping your \`${wrappedComponentName}\` component.`);
        } else if (isFunction(store.getInitialState)) {
          this.state[key] = store.getInitialState();
        }
      });
    }

    componentDidMount() {
      // Listening to each store.
      Object.keys(stores).forEach((key) => {
        const store = stores[key];
        const cb = (v) => {
          const newState = {};
          newState[key] = v;
          this.setState(newState);
        };

        this.listenTo(store, cb);
      });
    }

    shouldComponentUpdate(nextProps, nextState) {
      const thisChildProps = this._genProps(this.state);
      const nextChildProps = this._genProps(nextState);

      return !(isDeepEqual(thisChildProps, nextChildProps) && isDeepEqual(this.props, nextProps, _isEqual));
    }

    _genProps = (state) => {
      const storeProps = {};
      Object.keys(stores).forEach((key) => {
        storeProps[key] = state[key];
      });
      return mapProps(storeProps);
    };

    render() {
      const nextProps = this._genProps(this.state);

      return <Component {...nextProps} {...this.props} />;
    }
  }
  ConnectStoresWrapper.displayName = `ConnectStoresWrapper[${wrappedComponentName}] stores=${Object.keys(stores)}`;

  return ConnectStoresWrapper;
};
