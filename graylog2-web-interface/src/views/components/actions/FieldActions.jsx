// @flow strict
import * as React from 'react';
import { MenuItem } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';
import uuid from 'uuid/v4';

import FieldType from 'views/logic/fieldtypes/FieldType';
import { ActionContext } from 'views/logic/ActionContext';
import type { QueryId } from 'views/logic/queries/Query';
import type { FieldActionHandler } from 'views/logic/fieldactions/FieldActionHandler';
import OverlayDropdown from '../OverlayDropdown';
import style from '../Field.css';

type HandlerValueAction = {|
  handler: FieldActionHandler,
|}

type ComponentValueAction = {|
  title: string,
  component: React.AbstractComponent<*>,
|}

type FieldActionDefinition = HandlerValueAction | ComponentValueAction;

type Props = {|
  children: React.Node,
  disabled: boolean,
  element: React.Node,
  menuContainer: ?HTMLElement,
  name: string,
  queryId: QueryId,
  type: FieldType,
|};

type State = {
  open: boolean,
  overflowingComponents: { [string]: React.Node },
}

class FieldActions extends React.Component<Props, State> {
  static contextType = ActionContext;

  constructor(props: Props, context: typeof ActionContext) {
    super(props, context);
    this.state = {
      open: false,
      overflowingComponents: {},
    };
  }

  _createHandlerFor = (action: FieldActionDefinition): FieldActionHandler => {
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
        return Promise.resolve();
      };
    }
    throw new Error(`Invalid binding for action: ${String(action)} - has neither 'handler' nor 'component'.`);
  };

  _onMenuToggle = () => this.setState(state => ({ open: !state.open }));

  render() {
    const { children, disabled, element, menuContainer, name, type, queryId } = this.props;
    const { open } = this.state;
    const activeClass = open ? style.active : '';
    const disabledClass = disabled ? style.disabled : '';
    const wrappedElement = <span className={`field-element ${activeClass} ${disabledClass}`}>{element}</span>;
    const fieldActions = PluginStore.exports('fieldActions')
      .filter((action) => {
        const hide = action.hide || (() => false);
        return !hide(this.context);
      })
      .map((action) => {
        const handler = this._createHandlerFor(action);
        const onSelect = (event) => {
          this._onMenuToggle();
          handler(queryId, event.field, type, this.context);
        };

        const condition = action.condition || (() => true);
        const actionDisabled = !condition({ name, type, context: this.context });
        return (
          <MenuItem key={`${name}-action-${action.type}`}
                    disabled={actionDisabled}
                    eventKey={{ action: action.type, field: name }}
                    onSelect={onSelect}>{action.title}
          </MenuItem>
        );
      });
    return (
      <OverlayDropdown show={open}
                       toggle={wrappedElement}
                       placement="right"
                       onToggle={this._onMenuToggle}
                       menuContainer={menuContainer}>
        <div style={{ marginBottom: '10px' }}>
          <span className={`field-name ${style.dropdownheader}`}>
            {children}
          </span>
        </div>

        <MenuItem divider />
        <MenuItem header>Actions</MenuItem>
        {fieldActions}
      </OverlayDropdown>
    );
  }
}

export default FieldActions;
