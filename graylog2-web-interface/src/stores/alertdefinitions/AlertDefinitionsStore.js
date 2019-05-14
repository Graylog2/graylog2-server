import Reflux from 'reflux';
import URI from 'urijs';
import lodash from 'lodash';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

import AlertDefinitionsActions from 'actions/alertdefinitions/AlertDefinitionsActions';

const AlertDefinitionsStore = Reflux.createStore({
  listenables: [AlertDefinitionsActions],
  sourceUrl: '/plugins/org.graylog.events/events/processors',

  getInitialState() {
    return {
      list: [],
    };
  },

  alertDefinitionsUrl({ segments = [], query = {} }) {
    const uri = new URI(this.sourceUrl);
    const nextSegments = lodash.concat(uri.segment(), segments);
    uri.segmentCoded(nextSegments);
    uri.query(query);

    return URLUtils.qualifyUrl(uri.resource());
  },

  list() {
    // TODO: This needs to user proper pagination instead of requesting 1000 items
    const promise = fetch('GET', this.alertDefinitionsUrl({ query: { per_page: 1000 } }));

    promise.then((response) => {
      this.trigger({ list: response.event_processors });
      return response;
    });

    AlertDefinitionsActions.list.promise(promise);
  },

  get(alertDefinitionId) {
    const promise = fetch('GET', this.alertDefinitionsUrl({ segments: [alertDefinitionId] }));
    AlertDefinitionsActions.get.promise(promise);
  },

  create(alertDefinition) {
    const promise = fetch('POST', this.alertDefinitionsUrl({}), alertDefinition);
    promise.then(
      (response) => {
        UserNotification.success('Alert Definition created successfully', `Alert Definition "${alertDefinition.title}" was created successfully.`);
        this.list();
        return response;
      },
      (error) => {
        UserNotification.error(`Creating Alert Definition "${alertDefinition.title}" failed with status: ${error}`,
          'Could not save Alert Definition');
      },
    );
    AlertDefinitionsActions.create.promise(promise);
  },

  update(alertDefinitionId, alertDefinition) {
    const promise = fetch('PUT', this.alertDefinitionsUrl({ segments: [alertDefinitionId] }), alertDefinition);
    promise.then(
      (response) => {
        UserNotification.success('Alert Definition updated successfully', `Alert Definition "${alertDefinition.title}" was updated successfully.`);
        return response;
      },
      (error) => {
        UserNotification.error(`Updating Alert Definition "${alertDefinition.title}" failed with status: ${error}`,
          'Could not update Alert Definition');
      },
    );
    AlertDefinitionsActions.update.promise(promise);
  },

  delete(alertDefinition) {
    const promise = fetch('DELETE', this.alertDefinitionsUrl({ segments: [alertDefinition.id] }));

    promise.then(
      () => {
        UserNotification.success('Alert Definition deleted successfully', `Alert Definition "${alertDefinition.title}" was deleted successfully.`);
        this.list();
      },
      (error) => {
        UserNotification.error(`Deleting Alert Definition "${alertDefinition.title}" failed with status: ${error}`,
          'Could not delete Alert Definition');
      },
    );

    AlertDefinitionsActions.delete.promise(promise);
  },

  execute(alertDefinition, payload) {
    const promise = fetch('POST', this.alertDefinitionsUrl({ segments: [alertDefinition.id, 'execute'] }), {
      ...payload,
      type: alertDefinition.config.type, // Make sure to set correct type
    });

    promise.then(
      () => {
        UserNotification.success('Alert Definition executed successfully', `Alert Definition "${alertDefinition.title}" was executed successfully.`);
      },
      (error) => {
        UserNotification.error(`Executing Alert Definition "${alertDefinition.title}" failed with status: ${error}`,
          'Could not execute Alert Definition');
      },
    );

    AlertDefinitionsActions.execute.promise(promise);
  },
});

export default AlertDefinitionsStore;
