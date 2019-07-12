import Reflux from 'reflux';

const EventsActions = Reflux.createActions({
  search: { asyncResult: true },
});

export default EventsActions;
