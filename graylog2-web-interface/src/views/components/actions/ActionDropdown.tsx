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
import { useContext } from 'react';
import styled from 'styled-components';

import { MenuItem } from 'components/bootstrap';
import ActionMenuItem from 'views/components/actions/ActionMenuItem';
import type {
  ActionDefinition,
  ActionHandlerArguments,
  ActionComponents,
} from 'views/components/actions/ActionHandler';
import { Spinner } from 'components/common';
import useExternalValueActions from 'views/hooks/useExternalValueActions';
import FieldActionsContext from 'views/components/actions/FieldActionsContext';
import type { EvaluateActionCondition } from 'views/components/actions/FieldActionsContext';

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

const filterVisibleActions = (
  evaluateCondition: EvaluateActionCondition,
  handlerArgs: Props['handlerArgs'],
  actions: Array<ActionDefinition> | undefined = [],
) =>
  actions.filter((action: ActionDefinition) => {
    const { isHidden = () => false } = action;
    const hidden = evaluateCondition(isHidden, handlerArgs, false);

    return !hidden;
  });

const useInternalActions = (type: Props['type'], handlerArgs: Props['handlerArgs']) => {
  const { evaluateCondition, valueActions, fieldActions } = useContext(FieldActionsContext);
  const actions = type === 'value' ? valueActions : fieldActions;

  if (type === 'value') {
    return filterVisibleActions(evaluateCondition, handlerArgs, actions);
  }

  if (type === 'field') {
    return filterVisibleActions(evaluateCondition, handlerArgs, actions);
  }

  return [];
};

const useExternalActions = (type: Props['type'], handlerArgs: Props['handlerArgs']) => {
  const { isLoading, isError, externalValueActions } = useExternalValueActions();
  const { evaluateCondition } = useContext(FieldActionsContext);

  if (type !== 'value') {
    return { isLoading, isError, externalActions: [] };
  }

  const externalActions = filterVisibleActions(evaluateCondition, handlerArgs, externalValueActions);

  return { isLoading, isError, externalActions };
};

type Props = {
  children?: React.ReactNode;
  type: 'field' | 'value';
  handlerArgs: ActionHandlerArguments;
  setOverflowingComponents: (newComponents: ActionComponents) => void;
  overflowingComponents: ActionComponents;
  onMenuToggle: () => void;
};

const ActionDropdown = ({
  children = null,
  type,
  handlerArgs,
  setOverflowingComponents,
  overflowingComponents,
  onMenuToggle,
}: Props) => {
  const internalActions = useInternalActions(type, handlerArgs);
  const { externalActions, isLoading } = useExternalActions(type, handlerArgs);

  return (
    <>
      {children && (
        <>
          <StyledListItem>
            <DropdownHeader>{children}</DropdownHeader>
          </StyledListItem>
          <MenuItem divider />
        </>
      )}
      <MenuItem header>Actions</MenuItem>
      {internalActions.map((action) => (
        <ActionMenuItem
          action={action}
          key={`${type}-action-${action.type}`}
          handlerArgs={handlerArgs}
          setOverflowingComponents={setOverflowingComponents}
          overflowingComponents={overflowingComponents}
          type={type}
          onMenuToggle={onMenuToggle}
        />
      ))}
      {isLoading && (
        <>
          <MenuItem divider />
          <MenuItem disabled>
            <Spinner text="Loading" />
          </MenuItem>
        </>
      )}
      {!isLoading && externalActions && externalActions.length !== 0 && (
        <>
          <MenuItem divider />
          {externalActions.map((action) => (
            <ActionMenuItem
              action={action}
              key={`${type}-action-${action.type}`}
              handlerArgs={handlerArgs}
              setOverflowingComponents={setOverflowingComponents}
              overflowingComponents={overflowingComponents}
              type={type}
              onMenuToggle={onMenuToggle}
            />
          ))}
        </>
      )}
    </>
  );
};

export default ActionDropdown;
