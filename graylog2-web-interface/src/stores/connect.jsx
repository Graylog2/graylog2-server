import React from 'react';
import { ListenerMethods } from 'reflux';
import { isFunction } from 'lodash';

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
 * @returns {Object} - A React component wrapping the passed component.
 *
 * @example
 *
 * // Connecting the `SamplesComponent` class to the `SamplesStore`, hooking up its state to the `samples` prop and
 * // waiting for the store to settle down.
 *
 * connect(SamplesComponent, { samples: SamplesStore }, [SamplesActions.list])
 *
 */
export default (Component, stores) => {
  return class ConnectStoresWrapper extends React.Component {
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
        if (isFunction(store.getInitialState)) {
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

    render() {
      const storeProps = {};
      Object.keys(stores).forEach((key) => {
        storeProps[key] = this.state[key];
      });

      return <Component {...storeProps} {...this.props} />;
    }
  };
};
