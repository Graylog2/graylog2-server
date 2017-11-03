import { browserHistory } from 'react-router';

if (!window.graylogHistory) {
  window.graylogHistory = browserHistory;
}

export default window.graylogHistory;
