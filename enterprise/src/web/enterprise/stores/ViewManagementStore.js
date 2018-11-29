// @flow
import Reflux from 'reflux';

// $FlowFixMe: imports from core need to be fixed in flow
import fetch from 'logic/rest/FetchProvider';
// $FlowFixMe: imports from core need to be fixed in flow
import UserNotification from 'util/UserNotification';
// $FlowFixMe: imports from core need to be fixed in flow
import URLUtils from 'util/URLUtils';
import View from '../logic/views/View';
import Parameter from '../logic/parameters/Parameter';
import type { ViewJson } from '../logic/views/View';

type SortOrder = 'asc' | 'desc';

type SortField = 'id' | 'title' | 'created_at';

type PaginatedViews = {
  total: number,
  page: number,
  per_page: number,
  count: number,
  views: Array<View>,
};

export type ViewSummary = {
  id: string,
  title: string,
  description: string,
  summary: string,
  parameters: Array<Parameter>,
};

export type ViewSummaries = Array<ViewSummary>;

type ViewManagementActionsType = {
  delete: (View) => Promise<View>,
  forValue: () => Promise<ViewSummaries>,
  get: (string) => Promise<ViewJson>,
  save: (View) => Promise<View>,
  search: (string, number, number, SortField, SortOrder) => Promise<PaginatedViews>,
};

const ViewManagementActions: ViewManagementActionsType = Reflux.createActions({
  get: { asyncResult: true },
  save: { asyncResult: true },
  search: { asyncResult: true },
  delete: { asyncResult: true },
  forValue: { asyncResult: true },
});

const viewsUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/views');
const viewsIdUrl = id => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/views/${id}`);
const forValueUrl = () => URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/views/forValue');

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

  get(viewId: string): Promise<View> {
    const promise = fetch('GET', `${viewsUrl}/${viewId}`);
    ViewManagementActions.get.promise(promise);
    return promise;
  },

  save(view: View): void {
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

  forValue() {
    const promise = fetch('POST', forValueUrl())
      .catch(error => UserNotification.error(`Finding matching views for value failed with status: ${error}`, 'Could not find matching views'));
    ViewManagementActions.forValue.promise(promise);
  },
});

export { ViewManagementStore, ViewManagementActions };
