import Reflux from 'reflux';
import URI from 'urijs';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import CombinedProvider from 'injection/CombinedProvider';

const { CollectorConfigurationsActions } = CombinedProvider.get('CollectorConfigurations');

const CollectorConfigurationsStore = Reflux.createStore({
  listenables: [CollectorConfigurationsActions],
  sourceUrl: '/sidecar',
  configurations: undefined,
  pagination: {
    page: undefined,
    pageSize: undefined,
    total: undefined,
  },
  total: undefined,
  paginatedConfigurations: undefined,
  query: undefined,

  propagateChanges() {
    this.trigger({
      configurations: this.configurations,
      query: this.query,
      total: this.total,
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

  _fetchUploads({ page }) {
    const baseUrl = `${this.sourceUrl}/configurations/uploads`;
    const search = {
      page: page,
    };

    const uri = URI(baseUrl).search(search).toString();

    return fetch('GET', URLUtils.qualifyUrl(uri));
  },

  all() {
    const promise = this._fetchConfigurations({ pageSize: 0 });
    promise
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
    const promise = this._fetchConfigurations({ query: query, page: page, pageSize: pageSize });
    promise
      .then(
        (response) => {
          this.query = response.query;
          this.pagination = {
            page: response.pagination.page,
            pageSize: response.pagination.per_page,
            total: response.pagination.total,
          };
          this.total = response.total;
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

  listUploads({ page = 1 }) {
    const promise = this._fetchUploads({ page: page });
    promise
      .catch(
        (error) => {
          UserNotification.error(`Fetching configuration uploads failed with status: ${error}`,
            'Could not retrieve configurations');
        });

    CollectorConfigurationsActions.listUploads.promise(promise);
  },

  refreshList() {
    this.list({ query: this.query, page: this.page, pageSize: this.pageSize });
  },

  getConfiguration(configurationId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/configurations/${configurationId}`));
    promise.catch((error) => {
      let errorMessage = `Fetching Configuration failed with status: ${error}`;
      if (error.status === 404) {
        errorMessage = `Unable to find a Configuration with ID <${configurationId}>, please ensure it was not deleted.`;
      }
      UserNotification.error(errorMessage, 'Could not retrieve Configuration');
    });
    CollectorConfigurationsActions.getConfiguration.promise(promise);
  },

  getConfigurationSidecars(configurationId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/configurations/${configurationId}/sidecars`));
    promise.catch((error) => {
      let errorMessage = `Fetching Configuration failed with status: ${error}`;
      if (error.status === 404) {
        errorMessage = `Unable to find a Configuration with ID <${configurationId}>, please ensure it was not deleted.`;
      }
      UserNotification.error(errorMessage, 'Could not retrieve Configuration');
    });
    CollectorConfigurationsActions.getConfigurationSidecars.promise(promise);
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
        UserNotification.error(error.status === 400 ? error.responseMessage : `Creating configuration failed with status: ${error.message}`,
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
        UserNotification.error(`Updating Configuration failed: ${error.status === 400 ? error.responseMessage : error.message}`,
          `Could not update Configuration ${configuration.name}`);
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
        UserNotification.error(`Deleting Configuration failed: ${error.status === 400 ? error.responseMessage : error.message}`,
          `Could not delete Configuration ${configuration.name}`);
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
