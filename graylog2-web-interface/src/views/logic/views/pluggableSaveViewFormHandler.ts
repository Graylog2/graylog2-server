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
import type * as Immutable from 'immutable';

import type View from 'views/logic/views/View';
import UserNotification from 'util/UserNotification';
import type { SaveViewControls } from 'views/types';

const executeDuplicationHandler = async (view: View, userPermissions: Immutable.List<string>, duplicationHandlers: Array<(view: View, userPermissions: Immutable.List<string>) => Promise<View>>): Promise<View> => {
  let updatedView = view.toBuilder().build();

  // eslint-disable-next-line no-restricted-syntax
  for (const duplicationHandler of duplicationHandlers) {
    // eslint-disable-next-line no-await-in-loop,no-loop-func
    const viewWithPluginData = await duplicationHandler(updatedView, userPermissions).catch((e) => {
      const errorMessage = `An error occurred when executing a submit handler from a plugin: ${e}`;
      // eslint-disable-next-line no-console
      console.error(errorMessage);
      UserNotification.error(errorMessage);

      return updatedView;
    });

    if (viewWithPluginData) {
      updatedView = viewWithPluginData;
    }
  }

  return updatedView;
};

export const executePluggableSearchDuplicationHandler = (view: View, userPermissions: Immutable.List<string>, pluggableSaveViewControls: Array<SaveViewControls>) => {
  const pluginDuplicationHandlers = pluggableSaveViewControls?.map(({ onSearchDuplication }) => onSearchDuplication).filter((pluginData) => !!pluginData);

  return executeDuplicationHandler(view, userPermissions, pluginDuplicationHandlers);
};

export const executePluggableDashboardDuplicationHandler = (view: View, userPermissions: Immutable.List<string>, pluggableSaveViewControls: Array<SaveViewControls>) => {
  const pluginDuplicationHandlers = pluggableSaveViewControls?.map(({ onDashboardDuplication }) => onDashboardDuplication).filter((pluginData) => !!pluginData);

  return executeDuplicationHandler(view, userPermissions, pluginDuplicationHandlers);
};
