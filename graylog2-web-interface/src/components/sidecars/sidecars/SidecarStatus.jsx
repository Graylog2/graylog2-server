import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Col, Row } from 'react-bootstrap';

import SidecarStatusFileList from './SidecarStatusFileList';

const SidecarStatus = createReactClass({
  propTypes: {
    sidecar: PropTypes.object.isRequired,
  },

  componentDidMount() {
    this.style.use();
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  style: require('!style/useable!css!../styles/SidecarStyles.css'),

  formatNodeDetails(details) {
    if (!details) {
      return <p>Node details are currently unavailable. Please wait a moment and ensure the sidecar is correctly connected to the server.</p>;
    }
    return (
      <dl className="deflist top-margin">
        <dt>IP Address</dt>
        <dd>{details.ip}</dd>
        <dt>Operating System</dt>
        <dd>{details.operating_system}</dd>
        <dt>CPU Idle</dt>
        <dd>{lodash.isNumber(details.metrics.cpu_idle) ? `${details.metrics.cpu_idle}%` : 'Not available' }</dd>
        <dt>Load</dt>
        <dd>{lodash.defaultTo(details.metrics.load_1, 'Not available')}</dd>
        <dt>Volumes &gt; 75% full</dt>
        <dd>{details.metrics.disks_75.length > 0 ? details.metrics.disks_75.join(', ') : 'None'}</dd>
      </dl>
    );
  },

  formatCollectorStatus(details) {
    if (!details) {
      return <p>Collector statuses are currently unavailable. Please wait a moment and ensure the sidecar is correctly connected to the server.</p>;
    }

    const collectors = Object.keys(details.status.collectors);
    if (collectors.length === 0) {
      return <p>There are no collectors configured in this sidecar.</p>;
    }

    const statuses = [];
    collectors.forEach((collector) => {
      const status = details.status.collectors[collector];

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

      statuses.push(
        <dt key={`${collector}-key`} className={statusClass}>{collector}</dt>,
        <dd key={`${collector}-description`} className={statusClass}>{statusBadge}&ensp;{statusMessage}</dd>,
      );
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
            <h2>Collector statuses</h2>
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
