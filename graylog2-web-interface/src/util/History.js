import { createHistory } from 'history';

if (!window.graylogHistory) {
  window.graylogHistory = createHistory();
}

export default window.graylogHistory;
