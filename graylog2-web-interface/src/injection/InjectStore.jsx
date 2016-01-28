import React from 'react';
import Reflux from 'reflux';

const InjectStore = (storeName, key) => {
  return {
    componentWillUnmount: Reflux.connect().componentWillUnmount,
    contextTypes: {
      storeProvider: React.PropTypes.object,
    },
    getInitialState: function() {
      const store = this.context.storeProvider.getStore(storeName);
      return Reflux.connect(store, key).getInitialState();
    },

    componentDidMount: function() {
      const store = this.context.storeProvider.getStore(storeName);
      return Reflux.connect(store, key).componentDidMount.bind(this).apply();
    }
  };
};

export default InjectStore;
