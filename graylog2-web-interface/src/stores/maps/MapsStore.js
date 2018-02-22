import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

export const MapsActions = Reflux.createActions({
  'get': {asyncResult: true},
  'getMapData': {asyncResult: true},
});

export const MapsStore = Reflux.createStore({
  listenables: [MapsActions],
  mapCoordinates: undefined,

  getInitialState() {
    return {mapCoordinates: this.mapCoordinates};
  },

  getMapData(query, field, rangeType, rangeParams, streamId) {
    // TODO: For relative searches, the param is "range" and not "relative"...
    const timerange = rangeType === 'relative' ? {range: rangeParams.relative} : rangeParams;
    // Make sure the query param is not empty!
    const q = !query || query.length < 1 ? '*' : query;

    // The TimeRange object needs a type to correctly deserialize on the server.
    timerange.type = rangeType;

    const promise = fetch('POST', URLUtils.qualifyUrl(ApiRoutes.MapDataController.search().url), {
      query: q,
      timerange: timerange,
      limit: 50,
      stream_id: streamId,
      fields: [field],
    });

    promise.then(
      response => {
        this.trigger({mapCoordinates: response.fields[field]});
      },
      error => {
        let errorMessage;
        if (error.additional && error.additional.status === 400) {
          errorMessage = 'Map widget is only available for fields containing geo data.';
        } else {
          errorMessage = `Loading map information failed: ${error.message}`;
        }

        UserNotification.error(errorMessage, 'Could not load map information');
      }
    );

    MapsActions.getMapData.promise(promise);
  },
});
