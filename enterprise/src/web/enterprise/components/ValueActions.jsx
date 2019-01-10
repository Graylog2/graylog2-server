import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { MenuItem } from 'react-bootstrap';
import uuid from 'uuid/v4';

import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import OverlayDropdown from './OverlayDropdown';
import style from './Value.css';
import CustomPropTypes from './CustomPropTypes';

class ValueActions extends React.Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
    element: PropTypes.node.isRequired,
    field: PropTypes.string.isRequired,
    menuContainer: PropTypes.object,
    queryId: PropTypes.string.isRequired,
    type: CustomPropTypes.FieldType,
    value: PropTypes.node.isRequired,
  };

  static defaultProps = {
    menuContainer: document.body,
    type: FieldType.Unknown,
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      open: false,
      overflowingComponents: {},
    };
  }

  _createHandlerFor = (action) => {
    if (action.handler) {
      return action.handler;
    }
    if (action.component) {
      const ActionComponent = action.component;
      return (queryId, field, value) => {
        const id = uuid();
        const onClose = () => this.setState((state) => {
          const { overflowingComponents } = state;
          delete overflowingComponents[id];
          return { overflowingComponents };
        });
        const renderedComponent = <ActionComponent key={action.title} onClose={onClose} queryId={queryId} field={field} value={value} />;
        this.setState((state) => {
          const { overflowingComponents } = state;
          overflowingComponents[id] = renderedComponent;
          return { overflowingComponents };
        });
      };
    }
    throw new Error(`Invalid binding for action: ${action} - has neither 'handler' nor 'component'.`);
  };

  _onMenuToggle = () => this.setState(state => ({ open: !state.open }));

  render() {
    const { children, element, field, menuContainer, queryId, type, value } = this.props;
    const overflowingComponents = Object.values(this.state.overflowingComponents);
    const valueActions = PluginStore.exports('valueActions').map((valueAction) => {
      const handler = this._createHandlerFor(valueAction);
      const onSelect = (event) => {
        this._onMenuToggle();
        handler(queryId, event.field, event.value);
      };
      const condition = valueAction.condition || (() => true);
      const actionDisabled = !condition({ field, type, value });
      return (<MenuItem key={`value-action-${field}-${valueAction.type}`}
                        disabled={actionDisabled}
                        eventKey={{ field, value }}
                        onSelect={onSelect}>{valueAction.title}</MenuItem>);
    });
    return (
      <React.Fragment>
        <OverlayDropdown show={this.state.open}
                         toggle={element}
                         placement="right"
                         onToggle={this._onMenuToggle}
                         menuContainer={menuContainer}>
          <div className={style.bottomSpacer}>
            <span className={style.dropdownheader}>
              {children}
            </span>
          </div>

          <MenuItem divider />
          <MenuItem header>Actions</MenuItem>
          {valueActions}
        </OverlayDropdown>
        {overflowingComponents}
      </React.Fragment>
    );
  }
}

export default ValueActions;
