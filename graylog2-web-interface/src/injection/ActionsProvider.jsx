import CombinedProvider from 'injection/CombinedProvider';

class ActionsProvider {
  getActions(actionsName) {
    const result = CombinedProvider.get(actionsName);
    if (!result[`${actionsName}Actions`]) {
      throw new Error(`Requested actions "${actionsName}" is not registered.`);
    }
    return result[`${actionsName}Actions`];
  }
}

if (typeof window.actionsProvider === 'undefined') {
  window.actionsProvider = new ActionsProvider();
}

export default window.actionsProvider;
