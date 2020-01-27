// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { MenuItem } from 'components/graylog';

import FieldType from 'views/logic/fieldtypes/FieldType';
import { ActionContext } from 'views/logic/ActionContext';
import type { QueryId } from 'views/logic/queries/Query';
import OverlayDropdown from '../OverlayDropdown';
import style from '../Value.css';
import CustomPropTypes from '../CustomPropTypes';
import { createHandlerFor } from './ActionHandler';
import type { ActionDefinition, ActionHandlerCondition } from './ActionHandler';

type Props = {
  children: React.Node,
  element: React.Node,
  field: string,
  menuContainer: ?HTMLElement,
  oppositePlacement: Boolean,
  queryId: QueryId,
  type: FieldType,
  value: React.Node,
}

type State = {
  open: boolean,
  overflowingComponents: { [string]: React.Node },
}

class ValueActions extends React.Component<Props, State> {
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

  static contextType = ActionContext;

  constructor(props: Props, context: typeof ActionContext) {
    super(props, context);
    this.state = {
      open: false,
      overflowingComponents: {},
    };
  }

  _onMenuToggle = () => this.setState(state => ({ open: !state.open }));

  render() {
    const { children, element, field, menuContainer, oppositePlacement, queryId, type, value } = this.props;
    const { open, overflowingComponents: components } = this.state;
    // $FlowFixMe: Object.values signature is in the way for this one
    const overflowingComponents: Array<React.Node> = Object.values(components);
    const handlerArgs = { queryId, field, type, value, contexts: this.context };
    const valueActions: Array<ActionDefinition> = PluginStore.exports('valueActions')
      .filter((action: ActionDefinition) => {
        const { isHidden = (() => false: ActionHandlerCondition) } = action;
        return !isHidden(handlerArgs);
      })
      .map((action: ActionDefinition) => {
        const setActionComponents = fn => this.setState(({ overflowingComponents: actionComponents }) => ({ overflowingComponents: fn(actionComponents) }));
        const handler = createHandlerFor(action, setActionComponents);
        const onSelect = () => {
          this._onMenuToggle();
          handler(handlerArgs);
        };

        const { isEnabled = (() => true: ActionHandlerCondition) } = action;
        const actionDisabled = !isEnabled(handlerArgs);
        return (
          <MenuItem key={`value-action-${field}-${action.type}`}
                    disabled={actionDisabled}
                    eventKey={{ field, value }}
                    onSelect={onSelect}>{action.title}
          </MenuItem>
        );
      });

    console.log('element', element);
    return (
      <React.Fragment>
        <OverlayDropdown show={open}
                         toggle={element}
                         placement={oppositePlacement ? 'left' : 'right'}
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
