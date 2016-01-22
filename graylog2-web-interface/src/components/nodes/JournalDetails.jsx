import React, {PropTypes} from 'react';
import { ProgressBar, Row, Col } from 'react-bootstrap';
import numeral from 'numeral';
import moment from 'moment';
import {} from 'moment-duration-format';

import MetricsStore from 'stores/metrics/MetricsStore';
import { Timestamp } from 'components/common';
import jsRoutes from 'routing/jsRoutes';

const metricsStore = MetricsStore.instance;

const JournalDetails = React.createClass({
  propTypes: {
    nodeId: PropTypes.string.isRequired,
    enabled: PropTypes.boolean,
    directory: PropTypes.string.isRequired,
    maxSize: PropTypes.number.isRequired,
    maxAge: PropTypes.string.isRequired,
    flushInterval: PropTypes.number.isRequired,
    flushAge: PropTypes.string.isRequired,
  },
  getInitialState() {
    return ({
      initialized: false,
      hasError: false,
      append: 0,
      read: 0,
      segments: 0,
      entriesUncommitted: 0,
      size: 0,
      sizeLimit: 0,
      utilizationRatio: 0,
      oldestSegment: moment(),
    });
  },

  componentWillMount() {
    // only listen for updates if the journal is actually turned on
    if (this.props.enabled) {
      const metricNames = [
        'org.graylog2.journal.append.1-sec-rate',
        'org.graylog2.journal.read.1-sec-rate',
        'org.graylog2.journal.segments',
        'org.graylog2.journal.entries-uncommitted',
        'org.graylog2.journal.size',
        'org.graylog2.journal.size-limit',
        'org.graylog2.journal.utilization-ratio',
        'org.graylog2.journal.oldest-segment',
      ];
      metricsStore.listen({
        nodeId: this.props.nodeId,
        metricNames: metricNames,
        callback: (update, hasError) => {
          if (hasError) {
            this.setState({hasError: hasError});
            return;
          }
          // update is [{nodeId, values: [{name, value: {metric}}]} ...]
          // metric can be various different things, depending on metric {type: "GAUGE"|"COUNTER"|"METER"|"TIMER"}

          const newState = {
            initialized: true,
            hasError: hasError,
          };
          // we will only get one result, because we ask for only one node
          // get the base name from the metric name, and put the gauge metrics into our new state.
          update[0].values.forEach((namedMetric) => {
            const baseName = namedMetric.name.replace('org.graylog2.journal.', '').replace('.1-sec-rate', '');
            const camelCase = this.camelCase(baseName);
            newState[camelCase] = namedMetric.metric.value;
          });
          this.setState(newState);
        },
      });
    }
  },

  camelCase(input) {
    return input.toLowerCase().replace(/-(.)/g, (match, group1) => group1.toUpperCase());
  },

  render() {
    let content = null;
    if (this.props.enabled) {
      const oldestSegment = moment(this.state.oldestSegment);
      let overcommittedWarning = null;
      if (this.state.initialized && (this.state.utilizationRatio >= 1)) {
        overcommittedWarning = (
          <span>
            <strong>Warning!</strong> The journal utilization is exceeding the maximum size defined.
            <a href={jsRoutes.controllers.SystemController.index().url}> Click here</a> for more information.<br/>
          </span>
        );
      }

      content = (
        <div>
          <p className="description">Incoming messages are written to the disk journal to ensure they are kept safe in
            case of a server failure. The journal also helps keeping Graylog working if any of the outputs is too slow
            to keep up with the message rate
            or whenever there is a peak in incoming messages. It makes sure that Graylog does not buffer all of those
            messages in main memory and avoids overly long garbage collection pauses that way.
          </p>
          <Row className="row-sm">
            <Col md={6}>
              <h3>Configuration</h3>
              <dl className="system-journal">
                <dt>Path:</dt>
                <dd>{this.props.directory}</dd>
                <dt>Earliest entry:</dt>
                <dd><Timestamp dateTime={oldestSegment} relative/></dd>
                <dt>Maximum size:</dt>
                <dd>{numeral(this.props.maxSize).format('0,0 b')}</dd>
                <dt>Maximum age:</dt>
                <dd>{moment.duration(this.props.maxAge).format('d [days] h [hours] m [minutes]')}</dd>
                <dt>Flush policy:</dt>
                <dd>
                  Every {numeral(this.props.flushInterval).format('0,0')} messages
                  {' '}or {moment.duration(this.props.flushAge).format('h [hours] m [minutes] s [seconds]')}
                </dd>
              </dl>
            </Col>
            <Col md={6}>
              <h3>Utilization</h3>

              <ProgressBar now={this.state.utilizationRatio * 100}
                           label={numeral(this.state.utilizationRatio).format('0.0 %')}/>

              {overcommittedWarning}

              <strong>{numeral(this.state.entriesUncommitted).format('0,0')} unprocessed messages</strong>
              {' '}are currently in the journal, in {this.state.segments} segments.<br/>
              <strong>{numeral(this.state.append).format('0,0')} messages</strong>
              {' '}have been appended in the last second,{' '}
              <strong>{numeral(this.state.read).format('0,0')} messages</strong> have been read in the last second.
            </Col>
          </Row>
        </div>
      );
    } else {
      content = (<span>The disk journal is disabled for this node.</span>);
    }

    return (<div>
      <h2>Disk Journal</h2>
      {content}
    </div>);
  },
});

export default JournalDetails;
