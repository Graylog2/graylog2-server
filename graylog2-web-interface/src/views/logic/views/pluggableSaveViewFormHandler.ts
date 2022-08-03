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

export const executePluggableSearchDuplicationHandler = (view: View, userPermissions: Immutable.List<string>, pluggableSaveViewControlsFns: Array<() => SaveViewControls>) => {
  const pluginDuplicationHandlers = pluggableSaveViewControlsFns?.map((pluginFn) => pluginFn()?.onSearchDuplication).filter((pluginData) => !!pluginData);

  return executeDuplicationHandler(view, userPermissions, pluginDuplicationHandlers);
};

export const executePluggableDashboardDuplicationHandler = (view: View, userPermissions: Immutable.List<string>, pluggableSaveViewControlsFns: Array<() => SaveViewControls>) => {
  const pluginDuplicationHandlers = pluggableSaveViewControlsFns?.map((pluginFn) => pluginFn()?.onDashboardDuplication).filter((pluginData) => !!pluginData);

  return executeDuplicationHandler(view, userPermissions, pluginDuplicationHandlers);
};
