// @flow strict
import * as React from 'react';
import { MenuItem } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import FieldType from 'views/logic/fieldtypes/FieldType';
import { ActionContext } from 'views/logic/ActionContext';
import type { QueryId } from 'views/logic/queries/Query';
import OverlayDropdown from '../OverlayDropdown';
import style from '../Field.css';

type Props = {
  children: React.Node,
  disabled: boolean,
  element: React.Node,
  menuContainer: ?HTMLElement,
  name: string,
  queryId: QueryId,
  type: FieldType,
};

type State = {
  open: boolean,
}

class FieldActions extends React.Component<Props, State> {
  static contextType = ActionContext;

  constructor(props: Props, context: typeof ActionContext) {
    super(props, context);
    this.state = {
      open: false,
    };
  }

  _onMenuToggle = () => this.setState(state => ({ open: !state.open }));

  render() {
    const { children, disabled, element, menuContainer, name, type, queryId } = this.props;
    const { open } = this.state;
    const activeClass = open ? style.active : '';
    const disabledClass = disabled ? style.disabled : '';
    const wrappedElement = <span className={`field-element ${activeClass} ${disabledClass}`}>{element}</span>;
    const fieldActions = PluginStore.exports('fieldActions')
      .filter((fieldAction) => {
        const hide = fieldAction.hide || (() => false);
        return !hide(this.context);
      })
      .map((fieldAction) => {
        const onSelect = ({ field }) => {
          this._onMenuToggle();
          fieldAction.handler(queryId, field, type, this.context);
        };
        const condition = fieldAction.condition || (() => true);
        const actionDisabled = !condition({ name, type, context: this.context });
        return (
          <MenuItem key={`${name}-action-${fieldAction.type}`}
                    disabled={actionDisabled}
                    eventKey={{ action: fieldAction.type, field: name }}
                    onSelect={onSelect}>{fieldAction.title}
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
