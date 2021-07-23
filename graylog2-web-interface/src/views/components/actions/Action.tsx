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
import ActionMenuItem from 'views/components/actions/ActionMenuItem';
import { ActionDefinition, ActionHandlerArguments } from 'views/components/actions/ActionHandler';
import ExternalValueActionsContext from 'views/components/contexts/ExternalValueActionsContext';

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

const getInternalActionsPluginKey = (type: Props['type']) => {
  switch (type) {
    case 'value':
      return 'valueActions';
    case 'field':
      return 'fieldActions';
    default:
      return undefined;
  }
};

const useInternalActions = (type: Props['type'], handlerArgs: Props['handlerArgs']) => {
  const actionsPluginKey = getInternalActionsPluginKey(type);
  const internalActions = usePluginEntities(actionsPluginKey) ?? [];

  return internalActions.filter((action: ActionDefinition) => {
    const { isHidden = () => false } = action;

    return !isHidden(handlerArgs);
  });
};

const useExternalActions = (type: Props['type'], handlerArgs: Props['handlerArgs']) => {
  const externalValueActions = useContext(ExternalValueActionsContext);

  return type === 'value' ? externalValueActions?.getActionsForField(handlerArgs.field) : undefined;
};

const Action = ({ type, handlerArgs, menuContainer, element: Element, children }: Props) => {
  const [open, setOpen] = useState(false);
  const [overflowingComponents, setOverflowingComponents] = useState({});
  const internalActions = useInternalActions(type, handlerArgs);
  const externalActions = useExternalActions(type, handlerArgs);

  const _onMenuToggle = useCallback(() => setOpen(!open), [open]);
  const overflowingComponentsValues: Array<React.ReactNode> = Object.values(overflowingComponents);
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
        {internalActions.map((action) => (
          <ActionMenuItem action={action}
                          handlerArgs={handlerArgs}
                          setOverflowingComponents={setOverflowingComponents}
                          overflowingComponents={overflowingComponents}
                          type={type}
                          onMenuToggle={_onMenuToggle} />
        ))}

        {(externalActions && externalActions.length !== 0) && (
          <>
            <MenuItem divider />
            <MenuItem header>External Actions</MenuItem>
            {externalActions.map((action) => (
              <ActionMenuItem action={action}
                              handlerArgs={handlerArgs}
                              setOverflowingComponents={setOverflowingComponents}
                              overflowingComponents={overflowingComponents}
                              type={type}
                              onMenuToggle={_onMenuToggle} />
            ))}
          </>
        )}
      </OverlayDropdown>
      {overflowingComponentsValues}
    </>
  );
};

export default Action;
