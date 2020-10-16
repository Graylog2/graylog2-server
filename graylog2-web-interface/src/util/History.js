import { createBrowserHistory } from 'history';

if (!window.graylogHistory) {
  window.graylogHistory = createBrowserHistory();
}

export default window.graylogHistory;
