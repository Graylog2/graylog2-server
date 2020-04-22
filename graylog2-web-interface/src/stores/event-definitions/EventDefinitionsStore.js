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
  sourceUrl: '/events/definitions',
  all: undefined,
  eventDefinitions: undefined,
  context: undefined,
  query: undefined,
  pagination: {
    count: undefined,
    page: undefined,
    pageSize: undefined,
    total: undefined,
    grandTotal: undefined,
  },

  getInitialState() {
    return this.getState();
  },

  propagateChanges() {
    this.trigger(this.getState());
  },

  getState() {
    return {
      all: this.all,
      eventDefinitions: this.eventDefinitions,
      context: this.context,
      query: this.query,
      pagination: this.pagination,
    };
  },

  eventDefinitionsUrl({ segments = [], query = {} }) {
    const uri = new URI(this.sourceUrl);
    const nextSegments = lodash.concat(uri.segment(), segments);
    uri.segmentCoded(nextSegments);
    uri.query(query);

    return URLUtils.qualifyUrl(uri.resource());
  },

  refresh() {
    if (this.all) {
      this.listAll();
    }
    if (this.pagination.page) {
      this.listPaginated({
        query: this.query,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      });
    }
  },

  listAll() {
    const promise = fetch('GET', this.eventDefinitionsUrl({ query: { per_page: 0 } }));

    promise.then((response) => {
      this.all = response.event_definitions;
      this.context = response.context;
      this.propagateChanges();
      return response;
    });

    EventDefinitionsActions.listAll.promise(promise);
  },

  listPaginated({ query = '', page = 1, pageSize = 10 }) {
    const promise = fetch('GET', this.eventDefinitionsUrl({
      query: {
        query: query,
        page: page,
        per_page: pageSize,
      },
    }));

    promise.then((response) => {
      this.eventDefinitions = response.event_definitions;
      this.context = response.context;
      this.query = response.query;
      this.pagination = {
        count: response.count,
        page: response.page,
        pageSize: response.per_page,
        total: response.total,
        grandTotal: response.grand_total,
      };
      this.propagateChanges();
      return response;
    });

    EventDefinitionsActions.listPaginated.promise(promise);
  },

  get(eventDefinitionId) {
    const promise = fetch('GET', this.eventDefinitionsUrl({ segments: [eventDefinitionId] }));
    promise.catch((error) => {
      if (error.status === 404) {
        UserNotification.error(`Unable to find Event Definition with id <${eventDefinitionId}>, please ensure it wasn't deleted.`,
          'Could not retrieve Event Definition');
      }
    });
    EventDefinitionsActions.get.promise(promise);
  },

  setAlertFlag(eventDefinition) {
    const isAlert = eventDefinition.notifications.length > 0;
    return { ...eventDefinition, alert: isAlert };
  },

  create(eventDefinition) {
    const promise = fetch('POST', this.eventDefinitionsUrl({}), this.setAlertFlag(eventDefinition));
    promise.then(
      (response) => {
        UserNotification.success('Event Definition created successfully',
          `Event Definition "${eventDefinition.title}" was created successfully.`);
        this.refresh();
        return response;
      },
      (error) => {
        if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
          UserNotification.error(`Creating Event Definition "${eventDefinition.title}" failed with status: ${error}`,
            'Could not save Event Definition');
        }
      },
    );
    EventDefinitionsActions.create.promise(promise);
  },

  update(eventDefinitionId, eventDefinition) {
    const promise = fetch('PUT', this.eventDefinitionsUrl({ segments: [eventDefinitionId] }),
      this.setAlertFlag(eventDefinition));
    promise.then(
      (response) => {
        UserNotification.success('Event Definition updated successfully',
          `Event Definition "${eventDefinition.title}" was updated successfully.`);
        this.refresh();
        return response;
      },
      (error) => {
        if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
          UserNotification.error(`Updating Event Definition "${eventDefinition.title}" failed with status: ${error}`,
            'Could not update Event Definition');
        }
      },
    );
    EventDefinitionsActions.update.promise(promise);
  },

  delete(eventDefinition) {
    const promise = fetch('DELETE', this.eventDefinitionsUrl({ segments: [eventDefinition.id] }));

    promise.then(
      () => {
        UserNotification.success('Event Definition deleted successfully',
          `Event Definition "${eventDefinition.title}" was deleted successfully.`);
        this.refresh();
      },
      (error) => {
        UserNotification.error(`Deleting Event Definition "${eventDefinition.title}" failed with status: ${error}`,
          'Could not delete Event Definition');
      },
    );

    EventDefinitionsActions.delete.promise(promise);
  },

  enable(eventDefinition) {
    const promise = fetch('PUT', this.eventDefinitionsUrl({ segments: [eventDefinition.id, 'schedule'] }));
    promise.then(
      (response) => {
        UserNotification.success('Event Definition successfully enabled',
          `Event Definition "${eventDefinition.title}" was successfully enabled.`);
        this.refresh();
        return response;
      },
      (error) => {
        if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
          UserNotification.error(`Enabling Event Definition "${eventDefinition.title}" failed with status: ${error}`,
            'Could not enable Event Definition');
        }
      },
    );
    EventDefinitionsActions.enable.promise(promise);
  },

  disable(eventDefinition) {
    const promise = fetch('PUT', this.eventDefinitionsUrl({ segments: [eventDefinition.id, 'unschedule'] }));
    promise.then(
      (response) => {
        UserNotification.success('Event Definition successfully disabled',
          `Event Definition "${eventDefinition.title}" was successfully disabled.`);
        this.refresh();
        return response;
      },
      (error) => {
        if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
          UserNotification.error(`Disabling Event Definition "${eventDefinition.title}" failed with status: ${error}`,
            'Could not disable Event Definition');
        }
      },
    );
    EventDefinitionsActions.disable.promise(promise);
  },
});

export default EventDefinitionsStore;
