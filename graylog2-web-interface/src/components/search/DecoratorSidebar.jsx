import React from 'react';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import { AddDecoratorButton, Decorator } from 'components/search';
import { SortableList } from 'components/common';

import StoreProvider from 'injection/StoreProvider';
const DecoratorsStore = StoreProvider.getStore('Decorators');

import ActionsProvider from 'injection/ActionsProvider';
const DecoratorsActions = ActionsProvider.getActions('Decorators');

const DecoratorSidebar = React.createClass({
  propTypes: {
    stream: React.PropTypes.string,
  },
  mixins: [Reflux.connect(DecoratorsStore)],
  _formatDecorator(decorator) {
    const typeDefinition = this.state.types[decorator.type] || { requested_configuration: {}, name: `Unknown type: ${decorator.type}` };
    return ({ id: decorator._id, title: <Decorator key={`decorator-${decorator._id}`}
                                                   decorator={decorator}
                                                   typeDefinition={typeDefinition} /> });
  },
  _updateOrder(decorators) {
    decorators.map((item, idx) => {
      const decorator = this.state.decorators.find((i) => i._id === item.id);
      decorator.order = idx;
      DecoratorsActions.update(decorator._id, decorator);
    });
  },
  render() {
    if (!this.state.decorators) {
      return <Spinner />;
    }
    const decorators = this.state.decorators
      .filter((decorator) => (this.props.stream ? decorator.stream === this.props.stream : !decorator.stream))
      .sort((d1, d2) => d1.order - d2.order);
    const nextDecoratorOrder = decorators.length > 0 ? decorators[decorators.length - 1].order + 1 : 0;
    const decoratorItems = decorators.map(this._formatDecorator);
    return (
      <span>
        <AddDecoratorButton stream={this.props.stream} nextOrder={nextDecoratorOrder}/>
        <SortableList items={decoratorItems} onMoveItem={this._updateOrder} />
      </span>
    );
  },
});

export default DecoratorSidebar;
