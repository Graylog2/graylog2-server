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
import styled from 'styled-components';
import { useCallback, useContext, useState } from 'react';

import { MenuItem } from 'components/graylog';
import usePluginEntities from 'views/logic/usePluginEntities';
import { ActionDefinition, ActionHandlerArguments, createHandlerFor } from 'views/components/actions/ActionHandler';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

import OverlayDropdown from '../OverlayDropdown';

type Props = {
  children: React.ReactNode,
  element: React.ComponentType<{ active: boolean }>,
  handlerArgs: ActionHandlerArguments,
  menuContainer: HTMLElement | undefined | null,
  type: 'field' | 'value',
};

const DropdownHeader = styled.span`
    padding-left: 10px;
    padding-right: 10px;
    padding-bottom: 5px;
    margin-bottom: 5px;
    font-weight: 600;
`;
const StyledListItem = styled.li`
    margin-bottom: 10px;
    list-style: none;
`;

const Action = ({ type, handlerArgs, menuContainer, element: Element, children }: Props) => {
  const { unsetWidgetFocusing } = useContext(WidgetFocusContext);
  const [open, setOpen] = useState(false);
  const [overflowingComponents, setOverflowingComponents] = useState({});
  const _onMenuToggle = useCallback(() => setOpen(!open), [open]);
  const actions = usePluginEntities(`${type}Actions`);
  const overflowingComponentsValues: Array<React.ReactNode> = Object.values(overflowingComponents);

  const menuItems = actions
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
          unsetWidgetFocusing();
        }

        _onMenuToggle();
        handler(handlerArgs);
      };

      const { isEnabled = () => true } = action;
      const actionDisabled = !isEnabled(handlerArgs);

      const { field } = handlerArgs;

      return (
        <MenuItem key={`${type}-action-${action.type}`}
                  disabled={actionDisabled}
                  eventKey={{ action: type, field }}
                  onSelect={onSelect}>{action.title}
        </MenuItem>
      );
    });

  const element = <Element active={open} />;

  return (
    <>
      <OverlayDropdown show={open}
                       toggle={element}
                       placement="right"
                       onToggle={_onMenuToggle}
                       menuContainer={menuContainer}>
        <StyledListItem>
          <DropdownHeader>
            {children}
          </DropdownHeader>
        </StyledListItem>

        <MenuItem divider />
        <MenuItem header>Actions</MenuItem>
        {menuItems}
      </OverlayDropdown>
      {overflowingComponentsValues}
    </>
  );
};

export default Action;
