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

import Spinner from 'components/common/Spinner';
import usePluginEntities from 'views/logic/usePluginEntities';
import { MenuItem } from 'components/bootstrap';
import ActionMenuItem from 'views/components/actions/ActionMenuItem';
import type { ActionDefinition, ActionHandlerArguments } from 'views/components/actions/ActionHandler';

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

const filterVisibleActions = (handlerArgs: Props['handlerArgs'], actions: Array<ActionDefinition> | undefined = []) => {
  return actions.filter((action: ActionDefinition) => {
    const { isHidden = () => false } = action;

    return !isHidden(handlerArgs);
  });
};

const useInternalActions = (type: Props['type'], handlerArgs: Props['handlerArgs']) => {
  const valueActions = usePluginEntities('valueActions');
  const fieldActions = usePluginEntities('fieldActions');

  if (type === 'value') {
    return filterVisibleActions(handlerArgs, valueActions);
  }

  if (type === 'field') {
    return filterVisibleActions(handlerArgs, fieldActions);
  }

  return [];
};

const useExternalActions = (type: Props['type'], handlerArgs: Props['handlerArgs']) => {
  const usePluginExternalActions = usePluginEntities('useExternalActions');

  if (usePluginExternalActions && typeof usePluginExternalActions[0] === 'function') {
    const { isLoading, isError, externalValueActions } = usePluginExternalActions[0]();

    if (type !== 'value') {
      return { isLoading, isError, externalValueActions: [] };
    }

    const externalActions = filterVisibleActions(handlerArgs, externalValueActions);

    return { isLoading, isError, externalActions };
  }

  return { isLoading: false, isError: false, externalValueActions: [] };
};

type Props = {
  children: React.ReactNode,
  type: 'field' | 'value',
  handlerArgs: ActionHandlerArguments,
  setOverflowingComponents: (components: React.ReactNode) => void,
  overflowingComponents: React.ReactNode,
  onMenuToggle: () => void,
};

const ActionDropdown = ({
  children,
  type,
  handlerArgs,
  setOverflowingComponents,
  overflowingComponents,
  onMenuToggle,
}: Props) => {
  const internalActions = useInternalActions(type, handlerArgs);
  const { isLoading, externalActions } = useExternalActions(type, handlerArgs);

  return (
    <>
      <StyledListItem>
        <DropdownHeader>
          {children}
        </DropdownHeader>
      </StyledListItem>

      <MenuItem divider />
      <MenuItem header>Actions</MenuItem>
      {internalActions.map((action) => (
        <ActionMenuItem action={action}
                        key={`${type}-action-${action.type}`}
                        handlerArgs={handlerArgs}
                        setOverflowingComponents={setOverflowingComponents}
                        overflowingComponents={overflowingComponents}
                        type={type}
                        onMenuToggle={onMenuToggle} />
      ))}
      {isLoading && (<><MenuItem divider /><MenuItem disabled><Spinner text="Loading" /></MenuItem></>)}
      {(!isLoading && externalActions && externalActions.length !== 0) && (
        <>
          <MenuItem divider />
          {externalActions.map((action) => (
            <ActionMenuItem action={action}
                            key={`${type}-action-${action.type}`}
                            handlerArgs={handlerArgs}
                            setOverflowingComponents={setOverflowingComponents}
                            overflowingComponents={overflowingComponents}
                            type={type}
                            onMenuToggle={onMenuToggle} />
          ))}
        </>
      )}
    </>
  );
};

export default ActionDropdown;
