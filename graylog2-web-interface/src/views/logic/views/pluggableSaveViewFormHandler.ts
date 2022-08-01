import type * as Immutable from 'immutable';

import type View from 'views/logic/views/View';
import UserNotification from 'util/UserNotification';

const executeDuplicationHandler = async (view: View, userPermissions: Immutable.List<string>, duplicationHandlers): Promise<View> => {
  let updatedView = view.toBuilder().build();
  console.log('2', view, updatedView);

  // eslint-disable-next-line no-restricted-syntax
  for (const duplicationHandler of duplicationHandlers) {
    // eslint-disable-next-line no-await-in-loop,no-loop-func
    const entityWithPluginData = await duplicationHandler(updatedView, userPermissions).catch((e) => {
      const errorMessage = `An error occurred when executing a submit handler from a plugin: ${e}`;
      // eslint-disable-next-line no-console
      console.error(errorMessage);
      UserNotification.error(errorMessage);

      return updatedView;
    });

    if (entityWithPluginData) {
      updatedView = entityWithPluginData;
    }
  }

  return updatedView;
};

export const executePluggableSearchDuplicationHandler = (view: View, userPermissions, pluggableSaveViewControls) => {
  console.log('1', view);
  const pluginSubmitHandlers = pluggableSaveViewControls?.map((pluginFn) => pluginFn()?.onSearchDuplication).filter((pluginData) => !!pluginData);

  return executeDuplicationHandler(view, userPermissions, pluginSubmitHandlers);
};
