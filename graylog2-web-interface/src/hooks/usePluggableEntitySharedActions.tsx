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
import { useRef } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import type { EntitySharedAction, ModalHandler } from 'components/permissions/types';

function usePluggableEntitySharedActions<T>(entity: T, entityType: string) {
  const modalRefs = useRef({});
  const pluginActions = usePluginEntities('components.shared.entityActions');

  const availableActions = pluginActions.filter((action) => (action.useCondition ? !!action.useCondition() : true));

  const actions = availableActions.map((action: EntitySharedAction<T, ModalHandler>) => {
    const { key, component: PluggableEntityAction } = action;

    return (
      <PluggableEntityAction key={`entity-action-${key}`} entity={entity} modalRef={() => modalRefs.current[key]} />
    );
  });

  const actionModals = availableActions
    .filter(({ modal }) => !!modal)
    .map((action) => {
      const { key, modal: ActionModal } = action;

      return (
        <ActionModal
          entity={entity}
          entityType={entityType}
          key={`entity-action-modal-${key}`}
          ref={(ref) => {
            modalRefs.current[key] = ref;
          }}
        />
      );
    });

  return { actions, actionModals };
}

export default usePluggableEntitySharedActions;
