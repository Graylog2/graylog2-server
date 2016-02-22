import Reflux from 'reflux';

import jsRoutes from 'routing/jsRoutes';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

export const MapsActions = Reflux.createActions({
  'get': {asyncResult: true},
});

export const MapsStore = Reflux.createStore({
  listenables: [MapsActions],
  mapCoordinates: undefined,

  getInitialState() {
    return {mapCoordinates: this.mapCoordinates};
  },

  get(field) {
    const url = jsRoutes.controllers.api.UniversalSearchApiController.fieldTerms(
      'relative',
      'gl2_source_input:56c5a578bee8a99a998705a1',
      field,
      {range: 28800}
    ).url;

    const promise = fetch('GET', URLUtils.qualifyUrl(url));
    promise.then(
      response => {
        this.mapCoordinates = response;
        this.trigger({mapCoordinates: this.mapCoordinates});
      },
      error => {
        UserNotification.error(`Loading map information failed with status: ${error}`,
          'Could not load map information');
      }
    );

    MapsActions.get.promise(promise);
  },
});
