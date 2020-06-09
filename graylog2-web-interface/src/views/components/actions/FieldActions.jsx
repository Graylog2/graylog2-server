// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';
import { MenuItem } from 'components/graylog';
import { PluginStore } from 'graylog-web-plugin/plugin';

import FieldType from 'views/logic/fieldtypes/FieldType';
import { ActionContext } from 'views/logic/ActionContext';
import type { QueryId } from 'views/logic/queries/Query';
import { type ThemeInterface } from 'theme';
import OverlayDropdown from '../OverlayDropdown';
import style from '../Field.css';
import { createHandlerFor } from './ActionHandler';
import type { ActionComponents, ActionDefinition, ActionHandlerCondition } from './ActionHandler';

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
  overflowingComponents: ActionComponents,
};

type FieldElementProps = {
  active: boolean,
  disabled: boolean,
};

const FieldElement: StyledComponent<FieldElementProps, ThemeInterface, HTMLSpanElement> = styled.span.attrs({
  className: 'field-element',
})(({ active, disabled, theme }) => `
  color: ${active ? theme.colors.variant.info : 'currentColor'};
  opacity: ${disabled ? '0.3' : '1'};
`);

class FieldActions extends React.Component<Props, State> {
  static contextType = ActionContext;

  constructor(props: Props, context: typeof ActionContext) {
    super(props, context);
    this.state = {
      open: false,
      overflowingComponents: {},
    };
  }

  _onMenuToggle = () => this.setState((state) => ({ open: !state.open }));

  render() {
    const { children, disabled, element, menuContainer, name, type, queryId } = this.props;
    const { open } = this.state;

    const wrappedElement = <FieldElement active={open} disabled={disabled}>{element}</FieldElement>;
    const handlerArgs = { queryId, field: name, type, contexts: this.context };
    const fieldActions: Array<ActionDefinition> = PluginStore.exports('fieldActions')
      .filter((action: ActionDefinition) => {
        const { isHidden = (() => false: ActionHandlerCondition) } = action;
        return !isHidden(handlerArgs);
      })
      .map((action: ActionDefinition) => {
        const setActionComponents = (fn) => this.setState(({ overflowingComponents: actionComponents }) => ({ overflowingComponents: fn(actionComponents) }));
        const handler = createHandlerFor(action, setActionComponents);
        const onSelect = () => {
          this._onMenuToggle();
          handler(handlerArgs);
        };

        const { isEnabled = (() => true: ActionHandlerCondition) } = action;
        const actionDisabled = !isEnabled(handlerArgs);
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
