import Reflux from 'reflux';

import jsRoutes from 'routing/jsRoutes';
import URLUtils from 'util/URLUtils';
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

    const promise = fetch('POST', URLUtils.qualifyUrl('/plugins/org.graylog.plugins.map/mapdata'), {
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
      this._errorHandler('Loading map information failed:', 'Could not load map information')
    );

    MapsActions.getMapData.promise(promise);
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
        this.mapCoordinates = response.terms;
        this.trigger({mapCoordinates: this.mapCoordinates});
      },
      error => {
        UserNotification.error(`Loading map information failed with status: ${error}`,
          'Could not load map information');
      }
    );

    MapsActions.get.promise(promise);
  },

  _errorHandler(message, title) {
    return (error) => {
      let errorMessage;
      try {
        errorMessage = error.additional.body.message;
      } catch (e) {
        errorMessage = error.message;
      }
      UserNotification.error(`${message}: ${errorMessage}`, title);
    };
  },
});
