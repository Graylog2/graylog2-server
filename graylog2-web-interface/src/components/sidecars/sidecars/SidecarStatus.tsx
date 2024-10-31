/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useState } from 'react';
import defaultTo from 'lodash/defaultTo';
import isNumber from 'lodash/isNumber';

import { Col, Row, Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';
import commonStyles from 'components/sidecars/common/CommonSidecarStyles.css';
import type { SidecarSummary, Collector, NodeDetails } from 'components/sidecars/types';

import SidecarStatusFileList from './SidecarStatusFileList';
import VerboseMessageModal from './VerboseMessageModal';

type Props = {
  sidecar: SidecarSummary,
  collectors: Array<Collector>,
}

const formatNodeDetails = (details: NodeDetails) => {
  if (!details) {
    return <p>Node details are currently unavailable. Please wait a moment and ensure the sidecar is correctly connected to the server.</p>;
  }

  const { metrics } = details;

  return (
    <dl className={`${commonStyles.deflist} ${commonStyles.topMargin}`}>
      <dt>IP Address</dt>
      <dd>{defaultTo(details.ip, 'Not available')}</dd>
      <dt>Operating System</dt>
      <dd>{defaultTo(details.operating_system, 'Not available')}</dd>
      <dt>CPU Idle</dt>
      <dd>{isNumber(metrics?.cpu_idle) ? `${metrics?.cpu_idle}%` : 'Not available'}</dd>
      <dt>Load</dt>
      <dd>{defaultTo(metrics?.load_1, 'Not available')}</dd>
      <dt>Volumes &gt; 75% full</dt>
      {metrics?.disks_75 === undefined
        ? <dd>Not available</dd>
        : <dd>{metrics?.disks_75.length > 0 ? metrics?.disks_75.join(', ') : 'None'}</dd>}
    </dl>
  );
};

const formatCollectorStatus = (details: NodeDetails, collectors: Array<Collector>, _onShowVerbose: (name: string, verbose: string) => void) => {
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
    const collector = collectors.find((c) => c.id === status.collector_id);

    let statusMessage;
    let statusBadge;
    let statusClass;
    let verboseButton;

    switch (status.status) {
      case SidecarStatusEnum.RUNNING:
        statusMessage = 'Collector is running.';
        statusClass = 'text-success';
        statusBadge = <Icon name="play_arrow" />;
        break;
      case SidecarStatusEnum.FAILING:
        statusMessage = status.message;
        statusClass = 'text-danger';
        statusBadge = <Icon name="warning" />;

        if (status.verbose_message) {
          verboseButton = (
            <Button bsStyle="link"
                    bsSize="xs"
                    onClick={() => _onShowVerbose(collector.name, status.verbose_message)}>
              Show Details
            </Button>
          );
        }

        break;
      case SidecarStatusEnum.STOPPED:
        statusMessage = status.message;
        statusClass = 'text-danger';
        statusBadge = <Icon name="stop" />;
        break;
      default:
        statusMessage = 'Collector status is currently unknown.';
        statusClass = 'text-info';
        statusBadge = <Icon name="help" />;
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
};

const SidecarStatus = ({ sidecar, collectors }: Props) => {
  const [showVerboseModal, setShowVerboseModal] = useState(false);
  const [collectorName, setCollectorName] = useState('');
  const [collectorVerbose, setCollectorVerbose] = useState('');

  const _onShowVerbose = (name: string, verbose: string) => {
    setCollectorName(name);
    setCollectorVerbose(verbose);
    setShowVerboseModal(true);
  };

  const _onHideVerbose = () => {
    setShowVerboseModal(false);
  };

  const logFileList = sidecar.node_details.log_file_list || [];

  return (
    <div>
      <Row className="content">
        <Col md={12}>
          <h2>Node details</h2>
          {formatNodeDetails(sidecar.node_details)}
        </Col>
      </Row>
      <Row className="content">
        <Col md={12}>
          <h2>Collectors status</h2>
          <div className={commonStyles.topMargin}>
            {formatCollectorStatus(sidecar.node_details, collectors, _onShowVerbose)}
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
      <VerboseMessageModal showModal={showVerboseModal}
                           onHide={_onHideVerbose}
                           collectorName={collectorName}
                           collectorVerbose={collectorVerbose} />
    </div>
  );
};

export default SidecarStatus;
