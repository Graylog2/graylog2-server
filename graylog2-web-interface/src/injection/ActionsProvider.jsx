import CombinedProvider from 'injection/CombinedProvider';

/* global actionsProvider */

class ActionsProvider {
  getActions(actionsName) {
    const result = CombinedProvider.get(actionsName);
    if (!result[`${actionsName}Actions`]) {
      throw new Error(`Requested actions "${actionsName}" is not registered.`);
    }
    return result[`${actionsName}Actions`];
  }
}

if (typeof actionsProvider === 'undefined') {
  window.actionsProvider = new ActionsProvider();
}

export default actionsProvider;
