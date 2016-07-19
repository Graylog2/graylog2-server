import React from 'react';
import Reflux from 'reflux';
import { Well } from 'react-bootstrap';

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

        <Well style={{ marginTop: '10px' }}>
          <p className="description">
            Decorators can modify messages shown in the search results on the fly. These changes are not stored, but only
            shown in the search results. Decorator config is stored <strong>per stream</strong>.
          </p>
          <p className="description">
            Decorators are processed in order, from top to bottom. If you want to change the order in which decorators are
            processed, you can reorder them using drag and drop.
          </p>
        </Well>
      </span>
    );
  },
});

export default DecoratorSidebar;
