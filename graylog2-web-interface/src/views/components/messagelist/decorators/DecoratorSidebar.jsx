import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';

import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';
import DocsHelper from 'util/DocsHelper';
import { Button, OverlayTrigger, Popover } from 'components/graylog';
import { Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

import AddDecoratorButton from './AddDecoratorButton';
import Decorator from './Decorator';
import DecoratorList from './DecoratorList';
// eslint-disable-next-line import/no-webpack-loader-syntax
import DecoratorStyles from '!style!css!./decoratorStyles.css';

const { DecoratorsActions, DecoratorsStore } = CombinedProvider.get('Decorators');

class DecoratorSidebar extends React.Component {
  static propTypes = {
    decorators: PropTypes.array.isRequired,
    stream: PropTypes.string.isRequired,
    maximumHeight: PropTypes.number.isRequired,
  };

  constructor(props: P, context: any) {
    super(props, context);
    this.state = {
      maxDecoratorsHeight: 1000,
    };
  }

  componentDidMount() {
    DecoratorsActions.available();
    this._updateHeight();
    window.addEventListener('scroll', this._updateHeight);
  }

  componentDidUpdate(prevProps) {
    const { maximumHeight } = this.props;

    if (maximumHeight !== prevProps.maximumHeight) {
      this._updateHeight();
    }
  }

  componentWillUnmount() {
    window.removeEventListener('scroll', this._updateHeight);
  }

  _updateHeight = () => {
    const { maximumHeight } = this.props;
    const decoratorsContainer = ReactDOM.findDOMNode(this.decoratorsContainer);
    const maxHeight = maximumHeight - decoratorsContainer.getBoundingClientRect().top;

    this.setState({ maxDecoratorsHeight: Math.max(maxHeight, this.MINIMUM_DECORATORS_HEIGHT) });
  };

  _formatDecorator = (decorator) => {
    const { decorators, decoratorTypes, onChange } = this.props;
    const typeDefinition = decoratorTypes[decorator.type] || { requested_configuration: {}, name: `Unknown type: ${decorator.type}` };
    const deleteDecorator = decoratorId => onChange(decorators.filter(_decorator => _decorator.id !== decoratorId));
    const updateDecorator = (id, updatedDecorator) => onChange(decorators.map(_decorator => _decorator.id === id ? updatedDecorator : _decorator));
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
    const { decorators } = this.state;
    const { onChange } = this.props;
    orderedDecorators.forEach((item, idx) => {
      const decorator = decorators.find(i => i.id === item.id);
      decorator.order = idx;
    });

    onChange(decorators);
  };

  static MINIMUM_DECORATORS_HEIGHT = 50;

  render() {
    const { maxDecoratorsHeight } = this.state;
    const { decoratorTypes, onChange, stream, decorators } = this.props;
    if (!decoratorTypes) {
      return <Spinner />;
    }
    const sortedDecorators = decorators
      .filter(decorator => (stream ? decorator.stream === stream : !decorator.stream))
      .sort((d1, d2) => d1.order - d2.order);
    const nextDecoratorOrder = sortedDecorators.length > 0 ? sortedDecorators[sortedDecorators.length - 1].order + 1 : 0;
    const decoratorItems = sortedDecorators.map(this._formatDecorator);
    const popoverHelp = (
      <Popover id="decorators-help" className={DecoratorStyles.helpPopover}>
        <p className="description">
          Decorators can modify messages shown in the search results on the fly. These changes are not stored, but only
          shown in the search results. Decorator config is stored <strong>per stream</strong>.
        </p>
        <p className="description">
          Use drag and drop to modify the order in which decorators are processed.
        </p>
        <p>
          Read more about message decorators in the <DocumentationLink page={DocsHelper.PAGES.DECORATORS} text="documentation" />.
        </p>
      </Popover>
    );

    const addDecorator = decorator => onChange([...decorators, decorator]);

    return (
      <div>
        <AddDecoratorButton decoratorTypes={decoratorTypes} stream={stream} nextOrder={nextDecoratorOrder} onCreate={addDecorator} />
        <div className={DecoratorStyles.helpLinkContainer}>
          <OverlayTrigger trigger="click" rootClose placement="right" overlay={popoverHelp}>
            <Button bsStyle="link" className={DecoratorStyles.helpLink}>What are message decorators?</Button>
          </OverlayTrigger>
        </div>
        <div ref={(decoratorsContainer) => { this.decoratorsContainer = decoratorsContainer; }} className={DecoratorStyles.decoratorListContainer} style={{ maxHeight: maxDecoratorsHeight }}>
          <DecoratorList decorators={decoratorItems} onReorder={this._updateOrder} onChange={onChange} />
        </div>
      </div>
    );
  }
}

export default connect(DecoratorSidebar, { decoratorStore: DecoratorsStore }, ({ decoratorStore: { types = {} } = {} }) => ({ decoratorTypes: types }));
