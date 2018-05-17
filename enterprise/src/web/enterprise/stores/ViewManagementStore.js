import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';

const ViewManagementActions = Reflux.createActions({
  get: { asyncResult: true },
  save: { asyncResult: true },
  search: { asyncResult: true },
  delete: { asyncResult: true },
});

const viewsUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/views');
const viewsIdUrl = id => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/views/${id}`);

const ViewManagementStore = Reflux.createStore({
  listenables: [ViewManagementActions],

  views: undefined,
  pagination: {
    total: 0,
    count: 0,
    page: 1,
    perPage: 10,
  },

  getInitialState() {
    return {
      pagination: this.pagination,
      list: this.views,
    };
  },

  get(viewId) {
    const promise = fetch('GET', `${viewsUrl}/${viewId}`);
    ViewManagementActions.get.promise(promise);
    return promise;
  },

  save(view) {
    const promise = fetch('POST', viewsUrl, JSON.stringify(view));
    ViewManagementActions.save.promise(promise);
  },

  search(query, page = 1, perPage = 10, sortBy = 'title', order = 'asc') {
    const promise = fetch('GET', `${viewsUrl}?query=${query}&page=${page}&per_page=${perPage}&sort=${sortBy}&order=${order}`)
      .then((response) => {
        this.views = response.views;
        this.pagination = {
          total: response.total,
          count: response.count,
          page: response.page,
          perPage: response.per_page,
        };
        this.trigger({
          list: this.views,
          pagination: this.pagination,
        });

        return response;
      })
      .catch((error) => {
        UserNotification.error(`Fetching views failed with status: ${error}`,
          'Could not retrieve views');
      });

    ViewManagementActions.search.promise(promise);
  },

  delete(view) {
    const promise = fetch('DELETE', viewsIdUrl(view.id)).catch((error) => {
      UserNotification.error(`Deleting view ${view.title} failed with status: ${error}`,
        'Could not delete view');
    });

    ViewManagementActions.delete.promise(promise);
  },
});

export { ViewManagementStore, ViewManagementActions };
