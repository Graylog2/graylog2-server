import AlertsActions from 'actions/alerts/AlertsActions';
import AlertsStore from 'stores/alerts/AlertsStore';
import StreamStore from 'stores/streams/StreamsStore';
import DateTime from 'logic/datetimes/DateTime';

const AlertsAnnotator = {
  streams: undefined,

  initialize() {
    StreamStore.listStreams().then(streams => {
      this.streams = streams;
    });
  },

  fillAlertAnnotator(histogramData, rickshawAnnotator) {
    if (!histogramData || !histogramData[0] || !histogramData[0].x || !this.streams) {
      return;
    }

    const earliestDataPoint = histogramData[0].x;
    AlertsActions.listAllStreams.triggerPromise(earliestDataPoint).then(response => {
      response.alerts.forEach(alert => {
        const epoch = DateTime.fromUTCDateTime(alert.triggered_at).toMoment().unix();
        rickshawAnnotator.add(epoch, this._getAlertAnnotation(alert));
        rickshawAnnotator.update();
      });
    });
  },

  _getAlertAnnotation(alert) {
    const stream = this.streams.filter(s => s.id === alert.stream_id)[0];
    return `<i class='fa fa-warning'></i> Stream "${stream.title || 'Undefined'}" triggered an alert: ${alert.description}`;
  },
};

AlertsAnnotator.initialize();

export default AlertsAnnotator;
