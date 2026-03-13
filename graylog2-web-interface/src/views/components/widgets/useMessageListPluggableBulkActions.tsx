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

const useMessageListPluggableBulkActions = () => {
  const modalRefs = useRef({});
  const pluggableActions = usePluginEntities('views.components.widgets.messageTable.messageBulkActions');
  const availableActions = pluggableActions.filter((action) => (action.useCondition ? !!action.useCondition() : true));
  const actions = availableActions.map(({ component: PluggableMessageBulkAction, key }) => (
    <PluggableMessageBulkAction key={`bulk-message-action-${key}`} modalRef={() => modalRefs.current[key]} />
  ));

  const actionModals = availableActions
    .filter(({ modal }) => !!modal)
    .map(({ modal: ActionModal, key }) => (
      <ActionModal
        key={`bulk-message-action-modal-${key}`}
        ref={(r) => {
          modalRefs.current[key] = r;
        }}
      />
    ));

  return { actions, actionModals };
};

export default useMessageListPluggableBulkActions;
