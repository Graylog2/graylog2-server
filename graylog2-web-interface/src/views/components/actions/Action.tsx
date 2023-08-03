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
import { useCallback, useEffect, useMemo, useState } from 'react';

import type { ActionHandlerArguments, ActionComponents, ActionDefinition } from 'views/components/actions/ActionHandler';
import OverlayDropdown from 'components/common/OverlayDropdown';
import type { AppDispatch } from 'stores/useAppDispatch';
import useAppDispatch from 'stores/useAppDispatch';
import usePluginEntities from 'hooks/usePluginEntities';
import { ExternalActionsHookData } from 'views/types';

import ActionDropdown from './ActionDropdown';

type Props = {
  children: React.ReactNode,
  element: React.ComponentType<{ active: boolean }>,
  handlerArgs: ActionHandlerArguments,
  menuContainer: HTMLElement | undefined | null,
  type: 'field' | 'value',
};
const filterVisibleActions = (dispatch: AppDispatch, handlerArgs: Props['handlerArgs'], actions: Array<ActionDefinition> | undefined = []) => actions.filter((action: ActionDefinition) => {
  const { isHidden = () => false } = action;

  return dispatch((_dispatch, getState) => !isHidden(handlerArgs, getState));
});

const useInternalActions = (type: Props['type'], handlerArgs: Props['handlerArgs']) => {
  const valueActions = usePluginEntities('valueActions');
  const fieldActions = usePluginEntities('fieldActions');
  const dispatch = useAppDispatch();

  if (type === 'value') {
    return filterVisibleActions(dispatch, handlerArgs, valueActions);
  }

  if (type === 'field') {
    return filterVisibleActions(dispatch, handlerArgs, fieldActions);
  }

  return [];
};

const useExternalActions = (type: Props['type'], handlerArgs: Props['handlerArgs'], open) => {
  const usePluginExternalActions = usePluginEntities('useExternalActions');
  const dispatch = useAppDispatch();
  const [isLoading, setIsLoading] = useState(false);
  const [externalActions, setExternalActions] = useState<Array<ActionDefinition>>([]);

  const hasExternalActions = usePluginExternalActions && (typeof usePluginExternalActions?.[0] === 'function');
  const result = usePluginExternalActions[0]();

  if (open && hasExternalActions) {
    console.log({ hasExternalActions, open, 'usePluginExternalActions[0]': usePluginExternalActions[0] });

    console.log('!!!!!!!!!!!', type, { result, handlerArgs });
    const { isLoading: isActionLoading, externalValueActions } = result;
    setIsLoading(isActionLoading);

    if (type === 'value') {
      setExternalActions(filterVisibleActions(dispatch, handlerArgs, externalValueActions));
    }
  }

  return useMemo(() => ({ isLoading, externalActions }), [externalActions, isLoading]);
};

const Action = ({ type, handlerArgs, menuContainer, element: Element, children }: Props) => {
  const [open, setOpen] = useState(false);
  const [overflowingComponents, setOverflowingComponents] = useState<ActionComponents>({});
  const _onMenuToggle = useCallback(() => setOpen(!open), [open]);
  const overflowingComponentsValues: Array<React.ReactNode> = Object.values(overflowingComponents);
  const element = useMemo(() => <><Element active={open} /><span className="caret" /></>, [Element, open]);
  const internalActions = useInternalActions(type, handlerArgs);
  const { isLoading, externalActions } = useExternalActions(type, handlerArgs, open);

  useEffect(() => {
  }, [handlerArgs.field, menuContainer]);

  return (
    <>
      <OverlayDropdown show={open}
                       toggleChild={element}
                       placement="right"
                       onToggle={_onMenuToggle}
                       menuContainer={menuContainer}>
        {
          open && !isLoading && (
          <ActionDropdown handlerArgs={handlerArgs}
                          type={type}
                          setOverflowingComponents={setOverflowingComponents}
                          onMenuToggle={_onMenuToggle}
                          overflowingComponents={overflowingComponents}
                          externalActions={externalActions}
                          internalActions={internalActions}>
            {children}
          </ActionDropdown>
          )
        }
      </OverlayDropdown>
      {overflowingComponentsValues}
    </>
  );
};

export default Action;
