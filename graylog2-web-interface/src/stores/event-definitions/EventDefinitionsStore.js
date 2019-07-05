import Reflux from 'reflux';
import URI from 'urijs';
import lodash from 'lodash';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import CombinedProvider from 'injection/CombinedProvider';

const { EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');

const EventDefinitionsStore = Reflux.createStore({
  listenables: [EventDefinitionsActions],
  sourceUrl: '/plugins/org.graylog.events/events/processors',

  getInitialState() {
    return {
      list: [],
    };
  },

  eventDefinitionsUrl({ segments = [], query = {} }) {
    const uri = new URI(this.sourceUrl);
    const nextSegments = lodash.concat(uri.segment(), segments);
    uri.segmentCoded(nextSegments);
    uri.query(query);

    return URLUtils.qualifyUrl(uri.resource());
  },

  list() {
    // TODO: This needs to user proper pagination instead of requesting 1000 items
    const promise = fetch('GET', this.eventDefinitionsUrl({ query: { per_page: 1000 } }));

    promise.then((response) => {
      this.trigger({ list: response.event_processors });
      return response;
    });

    EventDefinitionsActions.list.promise(promise);
  },

  get(eventDefinitionId) {
    const promise = fetch('GET', this.eventDefinitionsUrl({ segments: [eventDefinitionId] }));
    EventDefinitionsActions.get.promise(promise);
  },

  create(eventDefinition) {
    const promise = fetch('POST', this.eventDefinitionsUrl({}), eventDefinition);
    promise.then(
      (response) => {
        UserNotification.success('Event Definition created successfully', `Event Definition "${eventDefinition.title}" was created successfully.`);
        this.list();
        return response;
      },
      (error) => {
        UserNotification.error(`Creating Event Definition "${eventDefinition.title}" failed with status: ${error}`,
          'Could not save Event Definition');
      },
    );
    EventDefinitionsActions.create.promise(promise);
  },

  update(eventDefinitionId, eventDefinition) {
    const promise = fetch('PUT', this.eventDefinitionsUrl({ segments: [eventDefinitionId] }), eventDefinition);
    promise.then(
      (response) => {
        UserNotification.success('Event Definition updated successfully', `Event Definition "${eventDefinition.title}" was updated successfully.`);
        return response;
      },
      (error) => {
        UserNotification.error(`Updating Event Definition "${eventDefinition.title}" failed with status: ${error}`,
          'Could not update Event Definition');
      },
    );
    EventDefinitionsActions.update.promise(promise);
  },

  delete(eventDefinition) {
    const promise = fetch('DELETE', this.eventDefinitionsUrl({ segments: [eventDefinition.id] }));

    promise.then(
      () => {
        UserNotification.success('Event Definition deleted successfully', `Event Definition "${eventDefinition.title}" was deleted successfully.`);
        this.list();
      },
      (error) => {
        UserNotification.error(`Deleting Event Definition "${eventDefinition.title}" failed with status: ${error}`,
          'Could not delete Event Definition');
      },
    );

    EventDefinitionsActions.delete.promise(promise);
  },
});

export default EventDefinitionsStore;
