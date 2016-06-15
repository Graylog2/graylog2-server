import React from 'react';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import { AddDecoratorButton, Decorator } from 'components/search';

import StoreProvider from 'injection/StoreProvider';
const DecoratorsStore = StoreProvider.getStore('Decorators');

import ActionsProvider from 'injection/ActionsProvider';
const DecoratorsActions = ActionsProvider.getActions('Decorators');

const DecoratorSidebar = React.createClass({
  propTypes: {
    stream: React.PropTypes.string,
  },
  mixins: [Reflux.connect(DecoratorsStore)],
  componentDidMount() {
    DecoratorsActions.list();
  },
  render() {
    if (!this.state.decorators) {
      return <Spinner />;
    }
    const decorators = this.state.decorators.filter((decorator) => (this.props.stream ? decorator.stream === this.props.stream : !decorator.stream));
    return (
      <span>
        <h3>Decorators</h3>
        <ul>
          {decorators.map(decorator =>
            <li key={`decorator-${decorator._id}`}>
              <Decorator decorator={decorator} />
            </li>)}
        </ul>
        <AddDecoratorButton stream={this.props.stream}/>
      </span>
    );
  },
});

export default DecoratorSidebar;
