import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import ReactDOM from 'react-dom';
import Reflux from 'reflux';
import { OverlayTrigger, Popover } from 'react-bootstrap';

import { Button } from 'components/graylog';
import { Spinner } from 'components/common';
import { AddDecoratorButton, Decorator, DecoratorList } from 'components/search';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import PermissionsMixin from 'util/PermissionsMixin';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

// eslint-disable-next-line import/no-webpack-loader-syntax
import DecoratorStyles from '!style!css!components/search/decoratorStyles.css';

const DecoratorsStore = StoreProvider.getStore('Decorators');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const DecoratorsActions = ActionsProvider.getActions('Decorators');

const DecoratorSidebar = createReactClass({
  displayName: 'DecoratorSidebar',

  propTypes: {
    stream: PropTypes.string.isRequired,
    maximumHeight: PropTypes.number.isRequired,
  },

  mixins: [Reflux.connect(DecoratorsStore), Reflux.connect(CurrentUserStore), PermissionsMixin],

  getInitialState() {
    return {
      maxDecoratorsHeight: 1000,
    };
  },

  componentDidMount() {
    this._updateHeight();
    window.addEventListener('scroll', this._updateHeight);
  },

  componentDidUpdate(prevProps) {
    const { maximumHeight } = this.props;

    if (maximumHeight !== prevProps.maximumHeight) {
      this._updateHeight();
    }
  },

  componentWillUnmount() {
    window.removeEventListener('scroll', this._updateHeight);
  },

  MINIMUM_DECORATORS_HEIGHT: 50,

  _updateHeight() {
    const { maximumHeight } = this.props;
    const decoratorsContainer = ReactDOM.findDOMNode(this.decoratorsContainer);
    const maxHeight = maximumHeight - decoratorsContainer.getBoundingClientRect().top;

    this.setState({ maxDecoratorsHeight: Math.max(maxHeight, this.MINIMUM_DECORATORS_HEIGHT) });
  },

  _formatDecorator(decorator) {
    const { types } = this.state;
    const typeDefinition = types[decorator.type] || { requested_configuration: {}, name: `Unknown type: ${decorator.type}` };
    return ({
      id: decorator.id,
      title: <Decorator key={`decorator-${decorator.id}`}
                        decorator={decorator}
                        typeDefinition={typeDefinition} />,
    });
  },

  _updateOrder(orderedDecorators) {
    const { decorators } = this.state;
    orderedDecorators.forEach((item, idx) => {
      const decorator = decorators.find(i => i.id === item.id);
      decorator.order = idx;
      DecoratorsActions.update(decorator.id, decorator);
    });
  },

  render() {
    const { currentUser, decorators, maxDecoratorsHeight } = this.state;
    const { stream } = this.props;
    if (!decorators) {
      return <Spinner />;
    }
    const sortedDecorators = decorators
      .filter(decorator => (stream ? decorator.stream === stream : !decorator.stream))
      .sort((d1, d2) => d1.order - d2.order);
    const nextDecoratorOrder = sortedDecorators.length > 0 ? sortedDecorators[decorators.length - 1].order + 1 : 0;
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

    const editPermissions = this.isPermitted(currentUser.permissions, `decorators:edit:${stream}`);
    return (
      <div>
        <AddDecoratorButton stream={stream} nextOrder={nextDecoratorOrder} disabled={!editPermissions} />
        <div className={DecoratorStyles.helpLinkContainer}>
          <OverlayTrigger trigger="click" rootClose placement="right" overlay={popoverHelp}>
            <Button bsStyle="link" className={DecoratorStyles.helpLink}>What are message decorators?</Button>
          </OverlayTrigger>
        </div>
        <div ref={(decoratorsContainer) => { this.decoratorsContainer = decoratorsContainer; }} className={DecoratorStyles.decoratorListContainer} style={{ maxHeight: maxDecoratorsHeight }}>
          <DecoratorList decorators={decoratorItems} onReorder={this._updateOrder} disableDragging={!editPermissions} />
        </div>
      </div>
    );
  },
});

export default DecoratorSidebar;
