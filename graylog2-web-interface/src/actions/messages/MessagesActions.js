import Reflux from 'reflux';

const MessagesActions = Reflux.createActions({
  loadMessage: { asyncResult: true },
  fieldTerms: { asyncResult: true },
});

export default MessagesActions;
