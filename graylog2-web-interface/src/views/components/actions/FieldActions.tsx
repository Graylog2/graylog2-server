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
import { useContext, useState, useCallback } from 'react';
import styled, { css } from 'styled-components';

import { MenuItem } from 'components/graylog';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { ActionContext } from 'views/logic/ActionContext';
import type { QueryId } from 'views/logic/queries/Query';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import usePluginEntities from 'views/logic/usePluginEntities';

import type { ActionDefinition } from './ActionHandler';
import { createHandlerFor } from './ActionHandler';

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

const FieldActions = ({ children, disabled, element, menuContainer, name, type, queryId }: Props) => {
  const actionContext = useContext(ActionContext);
  const { setWidgetFocusing } = useContext(WidgetFocusContext);
  const allFieldActions = usePluginEntities('fieldActions');

  const [open, setOpen] = useState(false);
  const [overflowingComponents, setOverflowingComponents] = useState({});

  const _onMenuToggle = useCallback(() => setOpen(!open), [open]);

  const wrappedElement = <FieldElement active={open} disabled={disabled}>{element}</FieldElement>;
  const handlerArgs = { queryId, field: name, type, contexts: actionContext };
  const fieldActions = allFieldActions
    .filter((action: ActionDefinition) => {
      const { isHidden = () => false } = action;

      return !isHidden(handlerArgs);
    })
    .map((action: ActionDefinition) => {
      const setActionComponents = (fn) => {
        setOverflowingComponents(fn(overflowingComponents));
      };

      const handler = createHandlerFor(action, setActionComponents);

      const onSelect = () => {
        const { resetFocus = false } = action;

        if (resetFocus) {
          setWidgetFocusing(undefined);
        }

        _onMenuToggle();
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
                     onToggle={_onMenuToggle}
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
};

export default FieldActions;
