import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Col, Row } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';
import SidecarStatusFileList from './SidecarStatusFileList';

const { CollectorsStore, CollectorsActions } = CombinedProvider.get('Collectors');

const SidecarStatus = createReactClass({
  mixins: [Reflux.connect(CollectorsStore)],

  propTypes: {
    sidecar: PropTypes.object.isRequired,
  },

  componentDidMount() {
    this.loadCollectors();
    this.style.use();
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  style: require('!style/useable!css!../styles/SidecarStyles.css'),

  loadCollectors() {
    CollectorsActions.all();
  },

  formatNodeDetails(details) {
    if (!details) {
      return <p>Node details are currently unavailable. Please wait a moment and ensure the sidecar is correctly connected to the server.</p>;
    }
    const metrics = details.metrics || {};
    return (
      <dl className="deflist top-margin">
        <dt>IP Address</dt>
        <dd>{lodash.defaultTo(details.ip, 'Not available')}</dd>
        <dt>Operating System</dt>
        <dd>{lodash.defaultTo(details.operating_system, 'Not available')}</dd>
        <dt>CPU Idle</dt>
        <dd>{lodash.isNumber(metrics.cpu_idle) ? `${metrics.cpu_idle}%` : 'Not available' }</dd>
        <dt>Load</dt>
        <dd>{lodash.defaultTo(metrics.load_1, 'Not available')}</dd>
        <dt>Volumes &gt; 75% full</dt>
        {metrics.disks_75 === undefined ?
          <dd>Not available</dd> :
          <dd>{metrics.disks_75.length > 0 ? metrics.disks_75.join(', ') : 'None'}</dd>
        }
      </dl>
    );
  },

  formatCollectorStatus(details) {
    const { collectors } = this.state;
    if (!details || !collectors) {
      return <p>Collectors status are currently unavailable. Please wait a moment and ensure the sidecar is correctly connected to the server.</p>;
    }

    if (!details.status) {
      return <p>Did not receive collectors status, set the option <code>send_status: true</code> in the sidecar configuration to see this information.</p>;
    }

    const collectorStatuses = details.status.collectors;
    if (collectorStatuses.length === 0) {
      return <p>There are no collectors configured in this sidecar.</p>;
    }

    const statuses = [];
    collectorStatuses.forEach((status) => {
      const collector = collectors.find(collector => collector.id === status.collector_id);

      let statusMessage;
      let statusBadge;
      let statusClass;
      switch (status.status) {
        case 0:
          statusMessage = 'Collector is running.';
          statusClass = 'text-success';
          statusBadge = <i className="fa fa-play fa-fw" />;
          break;
        case 2:
          statusMessage = status.message;
          statusClass = 'text-danger';
          statusBadge = <i className="fa fa-warning fa-fw" />;
          break;
        default:
          statusMessage = 'Collector status is currently unknown.';
          statusClass = 'text-info';
          statusBadge = <i className="fa fa-question-circle fa-fw" />;
      }

      if (collector) {
        statuses.push(
          <dt key={`${collector}-key`} className={statusClass}>{collector.name}</dt>,
          <dd key={`${collector}-description`} className={statusClass}>{statusBadge}&ensp;{statusMessage}</dd>,
        );
      }
    });

    return (
      <dl className="deflist">
        {statuses}
      </dl>
    );
  },

  render() {
    const sidecar = this.props.sidecar;

    const logFileList = sidecar.node_details.log_file_list || [];

    return (
      <div>
        <Row className="content">
          <Col md={12}>
            <h2>Node details</h2>
            {this.formatNodeDetails(sidecar.node_details)}
          </Col>
        </Row>
        <Row className="content">
          <Col md={12}>
            <h2>Collectors status</h2>
            <div className="top-margin">
              {this.formatCollectorStatus(sidecar.node_details)}
            </div>
          </Col>
        </Row>
        <Row className="content" hidden={logFileList.length === 0}>
          <Col md={12}>
            <h2>Log Files</h2>
            <p>Recently modified files will be highlighted in blue.</p>
            <div className="top-margin">
              <SidecarStatusFileList files={logFileList} />
            </div>
          </Col>
        </Row>
      </div>
    );
  },

});

export default SidecarStatus;
