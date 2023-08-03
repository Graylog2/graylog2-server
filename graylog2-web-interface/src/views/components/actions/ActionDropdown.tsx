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
import usePluginEntities from 'hooks/usePluginEntities';
import { MenuItem } from 'components/bootstrap';
import ActionMenuItem from 'views/components/actions/ActionMenuItem';
import type { ActionDefinition, ActionHandlerArguments, ActionComponents } from 'views/components/actions/ActionHandler';
import type { AppDispatch } from 'stores/useAppDispatch';
import useAppDispatch from 'stores/useAppDispatch';

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

type Props = {
  children: React.ReactNode,
  type: 'field' | 'value',
  handlerArgs: ActionHandlerArguments,
  setOverflowingComponents: (components: ActionComponents) => void,
  overflowingComponents: ActionComponents,
  onMenuToggle: () => void,
  internalActions: Array<ActionDefinition>,
  externalActions: Array<ActionDefinition>,
};

const ActionDropdown = ({
  children,
  type,
  handlerArgs,
  setOverflowingComponents,
  overflowingComponents,
  onMenuToggle,
  internalActions,
  externalActions,
}: Props) => (
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

    {(internalActions && internalActions.length !== 0) && (
    <>
      <MenuItem divider />
      {internalActions.map((action) => (
        <ActionMenuItem action={action}
                        key={`${type}-action-external-${action.type}`}
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

export default ActionDropdown;
