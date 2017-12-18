import Store from 'logic/local-storage/Store';

export const getStateFromLocalStorage = () => {
  const sessionId = Store.get('sessionId');
  const username = Store.get('username');

  return { sessions: { frontend: {}, isLoggedIn: false, sessionId: sessionId, username: username } };
};

let previousSession = getStateFromLocalStorage().sessions;

export const persistStateToLocalStorage = (state) => {
  try {
    const sessionsState = state.sessions;

    if (previousSession.sessionId !== sessionsState.sessionId) {
      // Now check the actual persisted state
      const persistedSessionsState = getStateFromLocalStorage().sessions;

      if (persistedSessionsState.sessionId !== sessionsState.sessionId) {
        if (sessionsState.sessionId) {
          Store.set('sessionId', sessionsState.sessionId);
          Store.set('username', sessionsState.username);
        } else if (persistedSessionsState.sessionId) {
          Store.delete('sessionId');
          Store.delete('username');
        }
      }
    }
    previousSession = sessionsState;
  } catch (e) {
    console.error('Could not persist session change', e);
  }
};
