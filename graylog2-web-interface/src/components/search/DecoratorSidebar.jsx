import React from 'react';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import { AddDecoratorButton, Decorator } from 'components/search';

import StoreProvider from 'injection/StoreProvider';
const DecoratorsStore = StoreProvider.getStore('Decorators');

const DecoratorSidebar = React.createClass({
  propTypes: {
    stream: React.PropTypes.string,
  },
  mixins: [Reflux.connect(DecoratorsStore)],
  _formatDecorator(decorator) {
    const typeDefinition = this.state.types[decorator.type] || { requested_configuration: {}, name: `Unknown type: ${decorator.type}` };
    return (<Decorator key={`decorator-${decorator._id}`}
                      decorator={decorator}
                      typeDefinition={typeDefinition} />);
  },
  render() {
    if (!this.state.decorators) {
      return <Spinner />;
    }
    const decorators = this.state.decorators.filter((decorator) => (this.props.stream ? decorator.stream === this.props.stream : !decorator.stream));
    return (
      <span>
        <AddDecoratorButton stream={this.props.stream}/>
        {decorators.map(decorator => this._formatDecorator(decorator))}
      </span>
    );
  },
});

export default DecoratorSidebar;
