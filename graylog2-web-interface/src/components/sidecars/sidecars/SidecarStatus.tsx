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
import isNumber from 'lodash/isNumber';
import { Trans, useTranslation } from 'react-i18next';
import i18n from 'i18n';

import { Col, Row, Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';
import commonStyles from 'components/sidecars/common/CommonSidecarStyles.css';
import type { SidecarSummary, Collector, NodeDetails } from 'components/sidecars/types';

import SidecarStatusFileList from './SidecarStatusFileList';
import VerboseMessageModal from './VerboseMessageModal';

type Props = {
  sidecar: SidecarSummary;
  collectors: Array<Collector>;
};

const formatNodeDetails = (details: NodeDetails) => {
  if (!details) {
    return (
      <p>{i18n.t('sidecars.sidecar_status.node_details_unavailable', { ns: 'sidecars' })}</p>
    );
  }

  const { metrics } = details;

  return (
    <dl className={`${commonStyles.deflist} ${commonStyles.topMargin}`}>
      <dt>{i18n.t('sidecars.sidecar_status.ip_address', { ns: 'sidecars' })}</dt>
      <dd>{details.ip ?? i18n.t('sidecars.sidecar_status.not_available', { ns: 'sidecars' })}</dd>
      <dt>{i18n.t('sidecars.sidecar_status.operating_system', { ns: 'sidecars' })}</dt>
      <dd>{details.operating_system ?? i18n.t('sidecars.sidecar_status.not_available', { ns: 'sidecars' })}</dd>
      <dt>{i18n.t('sidecars.sidecar_status.cpu_idle', { ns: 'sidecars' })}</dt>
      <dd>
        {isNumber(metrics?.cpu_idle)
          ? `${metrics?.cpu_idle}%`
          : i18n.t('sidecars.sidecar_status.not_available', { ns: 'sidecars' })}
      </dd>
      <dt>{i18n.t('sidecars.sidecar_status.load', { ns: 'sidecars' })}</dt>
      <dd>{metrics?.load_1 ?? i18n.t('sidecars.sidecar_status.not_available', { ns: 'sidecars' })}</dd>
      <dt>{i18n.t('sidecars.sidecar_status.volumes_75', { ns: 'sidecars' })}</dt>
      {metrics?.disks_75 === undefined ? (
        <dd>{i18n.t('sidecars.sidecar_status.not_available', { ns: 'sidecars' })}</dd>
      ) : (
        <dd>
          {metrics?.disks_75.length > 0
            ? metrics?.disks_75.join(', ')
            : i18n.t('sidecars.sidecar_status.none', { ns: 'sidecars' })}
        </dd>
      )}
    </dl>
  );
};

const formatCollectorStatus = (
  details: NodeDetails,
  collectors: Array<Collector>,
  _onShowVerbose: (name: string, verbose: string) => void,
) => {
  if (!details || !collectors) {
    return (
      <p>{i18n.t('sidecars.sidecar_status.collectors_status_unavailable', { ns: 'sidecars' })}</p>
    );
  }

  if (!details.status) {
    return (
      <p>
        <Trans
          i18n={i18n}
          ns="sidecars"
          i18nKey="sidecars.sidecar_status.no_status_received"
          components={{ code: <code /> }}
        />
      </p>
    );
  }

  const collectorStatuses = details.status.collectors;

  if (collectorStatuses.length === 0) {
    return <p>{i18n.t('sidecars.sidecar_status.no_collectors', { ns: 'sidecars' })}</p>;
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
        statusMessage = i18n.t('sidecars.sidecar_status.collector_running', { ns: 'sidecars' });
        statusClass = 'text-success';
        statusBadge = <Icon name="play_arrow" />;
        break;
      case SidecarStatusEnum.FAILING:
        statusMessage = status.message;
        statusClass = 'text-danger';
        statusBadge = <Icon name="warning" />;

        if (status.verbose_message) {
          verboseButton = (
            <Button bsStyle="link" bsSize="xs" onClick={() => _onShowVerbose(collector.name, status.verbose_message)}>
              {i18n.t('sidecars.sidecar_status.show_details', { ns: 'sidecars' })}
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
        statusMessage = i18n.t('sidecars.sidecar_status.collector_unknown', { ns: 'sidecars' });
        statusClass = 'text-info';
        statusBadge = <Icon name="help" />;
    }

    if (collector) {
      statuses.push(
        <dt key={`${collector.id}-key`} className={statusClass}>
          {collector.name}
        </dt>,
        <dd key={`${collector.id}-description`} className={statusClass}>
          {statusBadge}&ensp;{statusMessage}&ensp;{verboseButton}
        </dd>,
      );
    }
  });

  return <dl className={commonStyles.deflist}>{statuses}</dl>;
};

const SidecarStatus = ({ sidecar, collectors }: Props) => {
  const { t } = useTranslation('sidecars');
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
          <h2>{t('sidecars.sidecar_status.node_details_title')}</h2>
          {formatNodeDetails(sidecar.node_details)}
        </Col>
      </Row>
      <Row className="content">
        <Col md={12}>
          <h2>{t('sidecars.sidecar_status.collectors_status_title')}</h2>
          <div className={commonStyles.topMargin}>
            {formatCollectorStatus(sidecar.node_details, collectors, _onShowVerbose)}
          </div>
        </Col>
      </Row>
      <Row className="content" hidden={logFileList.length === 0}>
        <Col md={12}>
          <h2>{t('sidecars.sidecar_status.log_files_title')}</h2>
          <p className={commonStyles.topMargin}>{t('sidecars.sidecar_status.recently_modified')}</p>
          <div>
            <SidecarStatusFileList files={logFileList} />
          </div>
        </Col>
      </Row>
      <VerboseMessageModal
        showModal={showVerboseModal}
        onHide={_onHideVerbose}
        collectorName={collectorName}
        collectorVerbose={collectorVerbose}
      />
    </div>
  );
};

export default SidecarStatus;
