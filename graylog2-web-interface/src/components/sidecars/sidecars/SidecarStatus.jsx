import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Button, Col, Row } from 'react-bootstrap';

import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';
import commonStyles from 'components/sidecars/common/CommonSidecarStyles.css';

import SidecarStatusFileList from './SidecarStatusFileList';
import VerboseMessageModal from './VerboseMessageModal';

const SidecarStatus = createReactClass({
  propTypes: {
    sidecar: PropTypes.object.isRequired,
    collectors: PropTypes.array.isRequired,
  },

  getInitialState() {
    return { collectorName: '', collectorVerbose: '' };
  },

  formatNodeDetails(details) {
    if (!details) {
      return <p>Node details are currently unavailable. Please wait a moment and ensure the sidecar is correctly connected to the server.</p>;
    }
    const metrics = details.metrics || {};
    return (
      <dl className={`${commonStyles.deflist} ${commonStyles.topMargin}`}>
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

  formatCollectorStatus(details, collectors) {
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
      const collector = collectors.find(c => c.id === status.collector_id);

      let statusMessage;
      let statusBadge;
      let statusClass;
      let verboseButton;
      switch (status.status) {
        case SidecarStatusEnum.RUNNING:
          statusMessage = 'Collector is running.';
          statusClass = 'text-success';
          statusBadge = <i className="fa fa-play fa-fw" />;
          break;
        case SidecarStatusEnum.FAILING:
          statusMessage = status.message;
          statusClass = 'text-danger';
          statusBadge = <i className="fa fa-warning fa-fw" />;

          if (status.verbose_message) {
            verboseButton = (
              <Button bsStyle="link"
                      bsSize="xs"
                      onClick={() => this._onShowVerbose(collector.name, status.verbose_message)}>
                Show Details
              </Button>
            );
          }
          break;
        case SidecarStatusEnum.STOPPED:
          statusMessage = status.message;
          statusClass = 'text-danger';
          statusBadge = <i className="fa fa-stop fa-fw" />;
          break;
        default:
          statusMessage = 'Collector status is currently unknown.';
          statusClass = 'text-info';
          statusBadge = <i className="fa fa-question-circle fa-fw" />;
      }

      if (collector) {
        statuses.push(
          <dt key={`${collector.id}-key`} className={statusClass}>{collector.name}</dt>,
          <dd key={`${collector.id}-description`} className={statusClass}>{statusBadge}&ensp;{statusMessage}&ensp;{verboseButton}</dd>,
        );
      }
    });

    return (
      <dl className={commonStyles.deflist}>
        {statuses}
      </dl>
    );
  },

  _onShowVerbose(name, verbose) {
    this.setState({ collectorName: name, collectorVerbose: verbose });
    this.modal.open();
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
            <div className={commonStyles.topMargin}>
              {this.formatCollectorStatus(sidecar.node_details, this.props.collectors)}
            </div>
          </Col>
        </Row>
        <Row className="content" hidden={logFileList.length === 0}>
          <Col md={12}>
            <h2>Log Files</h2>
            <p className={commonStyles.topMargin}>Recently modified files will be highlighted in blue.</p>
            <div>
              <SidecarStatusFileList files={logFileList} />
            </div>
          </Col>
        </Row>
        <VerboseMessageModal ref={(c) => { this.modal = c; }}
                             collectorName={this.state.collectorName}
                             collectorVerbose={this.state.collectorVerbose} />,
      </div>
    );
  },

});

export default SidecarStatus;
