import React from 'react';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import { Decorator } from 'components/search';

import StoreProvider from 'injection/StoreProvider';
const DecoratorsStore = StoreProvider.getStore('Decorators');

import ActionsProvider from 'injection/ActionsProvider';
const DecoratorsActions = ActionsProvider.getActions('Decorators');

const DecoratorSidebar = React.createClass({
  mixins: [Reflux.connect(DecoratorsStore)],
  componentDidMount() {
    DecoratorsActions.list();
  },

  render() {
    if (!this.state.decorators) {
      return <Spinner />;
    }
    return (
      <span>
      <h3>Decorators</h3>
        <ul>
        {this.state.decorators.map(decorator => <li key={`decorator-${decorator._id}`}><Decorator decorator={decorator} /></li>)}
        </ul>
      </span>
    );
  },
});

export default DecoratorSidebar;
