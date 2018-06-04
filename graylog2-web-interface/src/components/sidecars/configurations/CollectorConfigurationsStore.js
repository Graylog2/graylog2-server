import Reflux from 'reflux';
import URI from 'urijs';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import CombinedProvider from 'injection/CombinedProvider';

const { CollectorConfigurationsActions } = CombinedProvider.get('CollectorConfigurations');

const CollectorConfigurationsStore = Reflux.createStore({
  listenables: [CollectorConfigurationsActions],
  sourceUrl: '/plugins/org.graylog.plugins.sidecar/sidecar',
  configurations: undefined,
  pagination: {
    page: undefined,
    pageSize: undefined,
    total: undefined,
  },
  paginatedConfigurations: undefined,
  query: undefined,

  propagateChanges() {
    this.trigger({
      configurations: this.configurations,
      query: this.query,
      pagination: this.pagination,
      paginatedConfigurations: this.paginatedConfigurations,
    });
  },

  _fetchConfigurations({ query, page, pageSize }) {
    const baseUrl = `${this.sourceUrl}/configurations`;
    const search = {
      query: query,
      page: page,
      per_page: pageSize,
    };

    const uri = URI(baseUrl).search(search).toString();

    return fetch('GET', URLUtils.qualifyUrl(uri));
  },

  all() {
    const promise = this._fetchConfigurations({ pageSize: 0 })
      .then(
        (response) => {
          this.configurations = response.configurations;
          this.propagateChanges();

          return response.configurations;
        },
        (error) => {
          UserNotification.error(`Fetching collector configurations failed with status: ${error}`,
            'Could not retrieve configurations');
        });

    CollectorConfigurationsActions.all.promise(promise);
  },

  list({ query = '', page = 1, pageSize = 10 }) {
    const promise = this._fetchConfigurations({ query: query, page: page, pageSize: pageSize })
      .then(
        (response) => {
          this.query = response.query;
          this.pagination = {
            page: response.page,
            pageSize: response.per_page,
            total: response.total,
          };
          this.paginatedConfigurations = response.configurations;
          this.propagateChanges();

          return response.configurations;
        },
        (error) => {
          UserNotification.error(`Fetching collector configurations failed with status: ${error}`,
            'Could not retrieve configurations');
        });

    CollectorConfigurationsActions.list.promise(promise);
  },

  refreshList() {
    this.list({ query: this.query, page: this.page, pageSize: this.pageSize });
  },

  getConfiguration(configurationId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/configurations/${configurationId}`));
    promise
      .catch(
        (error) => {
          UserNotification.error(`Fetching collector configuration failed with status: ${error}`,
            'Could not retrieve configuration');
        });
    CollectorConfigurationsActions.getConfiguration.promise(promise);
  },

  renderPreview(template) {
    const requestTemplate = {
      template: template,
    };

    const promise = fetch(
      'POST',
      URLUtils.qualifyUrl(`${this.sourceUrl}/configurations/render/preview`),
      requestTemplate);
    promise
      .catch(
        (error) => {
          UserNotification.error(`Fetching configuration preview failed with status: ${error}`,
            'Could not retrieve preview');
        });
    CollectorConfigurationsActions.renderPreview.promise(promise);
  },

  createConfiguration(configuration) {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/configurations`);
    const method = 'POST';

    const promise = fetch(method, url, configuration);
    promise
      .then((response) => {
        UserNotification.success('', 'Configuration successfully created');
        return response;
      }, (error) => {
        UserNotification.error(`Creating configuration failed with status: ${error.message}`,
          'Could not save configuration');
      });

    CollectorConfigurationsActions.createConfiguration.promise(promise);
  },

  updateConfiguration(configuration) {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/configurations/${configuration.id}`);

    const promise = fetch('PUT', url, configuration);
    promise
      .then((response) => {
        UserNotification.success('', 'Configuration successfully updated');
        this.refreshList();
        return response;
      }, (error) => {
        UserNotification.error(`Updating configuration failed with status: ${error.message}`,
          'Could not update configuration');
      });

    CollectorConfigurationsActions.updateConfiguration.promise(promise);
  },

  copyConfiguration(configurationId, name) {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/configurations/${configurationId}/${name}`);
    const method = 'POST';

    const promise = fetch(method, url);
    promise
      .then((response) => {
        UserNotification.success('', `Configuration "${name}" successfully copied`);
        this.refreshList();
        return response;
      }, (error) => {
        UserNotification.error(`Saving configuration "${name}" failed with status: ${error.message}`,
          'Could not save Configuration');
      });

    CollectorConfigurationsActions.copyConfiguration.promise(promise);
  },

  delete(configuration) {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/configurations/${configuration.id}`);
    const promise = fetch('DELETE', url);
    promise
      .then((response) => {
        UserNotification.success('', `Configuration "${configuration.name}" successfully deleted`);
        this.refreshList();
        return response;
      }, (error) => {
        UserNotification.error(`Deleting Output "${configuration.name}" failed with status: ${error.message}`,
          'Could not delete Configuration');
      });

    CollectorConfigurationsActions.delete.promise(promise);
  },

  validate(name) {
    const promise = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/configurations/validate/?name=${name}`));
    promise
      .then(
        response => response,
        error => (
          UserNotification.error(`Validating configuration with name "${name}" failed with status: ${error.message}`,
            'Could not validate configuration')
        ));

    CollectorConfigurationsActions.validate.promise(promise);
  },

});

export default CollectorConfigurationsStore;
