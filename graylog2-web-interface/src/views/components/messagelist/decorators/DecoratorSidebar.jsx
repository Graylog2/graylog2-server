import PropTypes from 'prop-types';
import React from 'react';

import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';
import { Spinner } from 'components/common';

import AddDecoratorButton from './AddDecoratorButton';
import Decorator from './Decorator';
import DecoratorList from './DecoratorList';
// eslint-disable-next-line import/no-webpack-loader-syntax
import DecoratorStyles from '!style!css!./decoratorStyles.css';

const { DecoratorsActions, DecoratorsStore } = CombinedProvider.get('Decorators');

class DecoratorSidebar extends React.Component {
  static propTypes = {
    decorators: PropTypes.array.isRequired,
    decoratorTypes: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    stream: PropTypes.string.isRequired,
  };

  componentDidMount() {
    DecoratorsActions.available();
  }

  _formatDecorator = (decorator) => {
    const { decorators, decoratorTypes, onChange } = this.props;
    const typeDefinition = decoratorTypes[decorator.type] || { requested_configuration: {}, name: `Unknown type: ${decorator.type}` };
    const deleteDecorator = decoratorId => onChange(decorators.filter(_decorator => _decorator.id !== decoratorId));
    const updateDecorator = (id, updatedDecorator) => onChange(decorators.map(_decorator => (_decorator.id === id ? updatedDecorator : _decorator)));
    return ({
      id: decorator.id,
      title: <Decorator key={`decorator-${decorator.id}`}
                        decorator={decorator}
                        decoratorTypes={decoratorTypes}
                        onDelete={deleteDecorator}
                        onUpdate={updateDecorator}
                        typeDefinition={typeDefinition} />,
    });
  };

  _updateOrder = (orderedDecorators) => {
    const { decorators, onChange } = this.props;
    orderedDecorators.forEach((item, idx) => {
      const decorator = decorators.find(i => i.id === item.id);
      decorator.order = idx;
    });

    onChange(decorators);
  };

  render() {
    const { decoratorTypes, onChange, stream, decorators } = this.props;
    if (!decoratorTypes) {
      return <Spinner />;
    }
    const sortedDecorators = decorators
      .filter(decorator => (stream ? decorator.stream === stream : !decorator.stream))
      .sort((d1, d2) => d1.order - d2.order);
    const nextDecoratorOrder = sortedDecorators.length > 0 ? sortedDecorators[sortedDecorators.length - 1].order + 1 : 0;
    const decoratorItems = sortedDecorators.map(this._formatDecorator);

    const addDecorator = decorator => onChange([...decorators, decorator]);

    return (
      <div>
        <AddDecoratorButton decoratorTypes={decoratorTypes} stream={stream} nextOrder={nextDecoratorOrder} onCreate={addDecorator} />
        <div ref={(decoratorsContainer) => { this.decoratorsContainer = decoratorsContainer; }} className={DecoratorStyles.decoratorListContainer}>
          <DecoratorList decorators={decoratorItems} onReorder={this._updateOrder} onChange={onChange} />
        </div>
      </div>
    );
  }
}

export default connect(DecoratorSidebar, { decoratorStore: DecoratorsStore }, ({ decoratorStore: { types = {} } = {} }) => ({ decoratorTypes: types }));
