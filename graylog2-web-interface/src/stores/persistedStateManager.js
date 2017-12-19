import Store from 'logic/local-storage/Store';
import { selectors } from 'ducks/sessions/sessions';

export const getStateFromLocalStorage = () => {
  const sessionId = Store.get('sessionId');
  const username = Store.get('username');

  return { sessions: { frontend: {}, isLoggedIn: false, sessionId: sessionId, username: username } };
};

let previousSession = getStateFromLocalStorage().sessions;

export const persistStateToLocalStorage = (state) => {
  try {
    const sessionsState = state.sessions;
    const sessionId = selectors.getSessionId(sessionsState);
    const username = selectors.getUsername(sessionsState);

    if (previousSession.sessionId !== sessionId) {
      // Now check the actual persisted state
      const persistedSessionsState = getStateFromLocalStorage().sessions;

      if (persistedSessionsState.sessionId !== sessionId) {
        if (sessionId) {
          Store.set('sessionId', sessionId);
          Store.set('username', username);
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
