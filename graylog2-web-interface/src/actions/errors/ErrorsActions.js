import Reflux from 'reflux';
import { singletonActions } from 'views/logic/singleton';

const ErrorsActions = singletonActions(
  'Errors',
  () => Reflux.createActions(['report']),
);

export default ErrorsActions;
