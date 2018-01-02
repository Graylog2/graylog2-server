import React from 'react';
import Reflux from 'reflux';
import createClass from 'create-react-class';

import Spinner from 'components/common/Spinner';

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
 * Upon mount of the generated component all functions included in `initialActions` will be executed. This is usable to
 * execute actions on one or more stores, which populate the store(s).
 *
 * When either `initialActions` is non-empty or `waitForStores` is set to `true`, the generated component will return a
 * spinner instead of the supplied component if at least one of the mapped props keys is `undefined` (meaning that at
 * least one state of the stores is `undefined`, usually meaning that it is not available yet).
 *
 * @param {Object} Component - The component which should be connected to the stores.
 * @param {Object} stores - A mapping of desired props keys to stores.
 * @param {function[]} initialActions - An array of functions being called when the generated component is mounted.
 * @param {boolean} [waitForStores=false] - Defines if the generated component waits for all specified keys in its state
 *                                        before rendering `Component`.
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
export default function connect(Component, stores = {}, initialActions = [], waitForStores = false) {
  const storeMixins = [];
  Object.keys(stores).forEach((key) => {
    storeMixins.push(Reflux.connect(stores[key], key));
  });

  const StoreConnectionWrapper = createClass({
    mixins: storeMixins,
    componentDidMount() {
      initialActions.forEach(action => action());
    },
    render() {
      const props = {};
      Object.keys(stores).forEach((key) => {
        props[key] = this.state[key];
      });

      if (waitForStores || initialActions.length > 0) {
        if (Object.keys(stores).map((key) => { return this.state[key] !== undefined; }).find(elem => elem === false) !== undefined) {
          return <Spinner />;
        }
      }
      return <Component {...props} />;
    },
  });

  return StoreConnectionWrapper;
}
