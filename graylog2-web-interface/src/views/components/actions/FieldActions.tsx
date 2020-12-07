/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import styled, { css } from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { MenuItem } from 'components/graylog';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { ActionContext } from 'views/logic/ActionContext';
import type { QueryId } from 'views/logic/queries/Query';
import { CurrentViewStateActions } from 'views/stores/CurrentViewStateStore';

import { createHandlerFor } from './ActionHandler';
import type { ActionComponents, ActionDefinition } from './ActionHandler';

import OverlayDropdown from '../OverlayDropdown';
import style from '../Field.css';

type Props = {
  children: React.ReactNode,
  disabled: boolean,
  element: React.ReactNode,
  menuContainer: HTMLElement | undefined | null,
  name: string,
  queryId: QueryId,
  type: FieldType,
};

type State = {
  open: boolean,
  overflowingComponents: ActionComponents,
};

type FieldElementProps = {
  active: boolean,
  disabled: boolean,
};

const FieldElement = styled.span.attrs({
  className: 'field-element', /* stylelint-disable-line property-no-unknown */
})<FieldElementProps>(({ active, disabled, theme }) => css`
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
        const { isHidden = () => false } = action;

        return !isHidden(handlerArgs);
      })
      .map((action: ActionDefinition) => {
        const setActionComponents = (fn) => this.setState(({ overflowingComponents: actionComponents }) => ({ overflowingComponents: fn(actionComponents) }));
        const handler = createHandlerFor(action, setActionComponents);

        const onSelect = () => {
          const { resetFocus = false } = action;

          if (resetFocus) {
            CurrentViewStateActions.focusWidget(undefined);
          }

          this._onMenuToggle();
          handler(handlerArgs);
        };

        const { isEnabled = () => true } = action;
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
