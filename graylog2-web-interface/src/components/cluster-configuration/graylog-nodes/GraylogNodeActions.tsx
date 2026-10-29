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
import React, { useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { LinkContainer, ConfirmDialog, IfPermitted } from 'components/common';
import { MoreActions } from 'components/common/EntityDataTable';
import { MenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { overrideLoadBalancerStatus } from 'api/system-load-balancer';
import { pauseProcessing, resumeProcessing } from 'api/system-processing';

import { LOAD_BALANCER_STATUS } from './fetchClusterGraylogNodes';
import type { ClusterGraylogNode as GraylogNode, LoadBalancerStatus } from './fetchClusterGraylogNodes';

type Props = {
  node: GraylogNode;
};

const GraylogNodeActions = ({ node }: Props) => {
  const { t } = useTranslation('system');
  const [showMessageProcessingModal, setShowMessageProcessingModal] = useState<boolean>(false);
  const [loadBalancerStatusToConfirm, setLoadBalancerStatusToConfirm] = useState<LoadBalancerStatus | undefined>(
    undefined,
  );

  const nodeName = `${node.short_node_id} / ${node.hostname}`;

  const toggleMessageProcessing = () => {
    if (node.is_processing) {
      pauseProcessing(node.node_id);
    } else {
      resumeProcessing(node.node_id);
    }
    setShowMessageProcessingModal(false);
  };

  const updateLoadBalancerStatus = (status: LoadBalancerStatus) => {
    overrideLoadBalancerStatus(node.node_id, status);
    setLoadBalancerStatusToConfirm(undefined);
  };

  return (
    <>
      <MoreActions>
        <IfPermitted permissions="processing:changestate">
          <MenuItem onSelect={() => setShowMessageProcessingModal(true)}>
            {node.is_processing
              ? t('graylog_node_actions.pause_message_processing')
              : t('graylog_node_actions.resume_message_processing')}
          </MenuItem>
        </IfPermitted>
        <IfPermitted permissions="lbstatus:change">
          {node.lb_status === LOAD_BALANCER_STATUS.ALIVE ? (
            <MenuItem onSelect={() => setLoadBalancerStatusToConfirm(LOAD_BALANCER_STATUS.DEAD)}>
              {t('graylog_node_actions.override_load_balancer_dead')}
            </MenuItem>
          ) : (
            <MenuItem onSelect={() => setLoadBalancerStatusToConfirm(LOAD_BALANCER_STATUS.ALIVE)}>
              {t('graylog_node_actions.override_load_balancer_alive')}
            </MenuItem>
          )}
        </IfPermitted>
        <IfPermitted permissions={['processing:changestate', 'lbstatus:change', 'node:shutdown']} anyPermissions>
          <IfPermitted permissions={['inputs:read', 'threads:dump']} anyPermissions>
            <MenuItem divider />
          </IfPermitted>
        </IfPermitted>
        <LinkContainer to={Routes.SYSTEM.METRICS(node.node_id)}>
          <MenuItem>{t('node_actions.metrics')}</MenuItem>
        </LinkContainer>
        <HideOnCloud>
          <IfPermitted permissions="inputs:read">
            <LinkContainer to={Routes.node_inputs(node.node_id)}>
              <MenuItem>{t('graylog_node_actions.local_message_inputs')}</MenuItem>
            </LinkContainer>
          </IfPermitted>
        </HideOnCloud>
        <IfPermitted permissions="threads:dump">
          <LinkContainer to={Routes.SYSTEM.THREADDUMP(node.node_id)}>
            <MenuItem>{t('node_actions.thread_dump')}</MenuItem>
          </LinkContainer>
        </IfPermitted>
        <IfPermitted permissions="processbuffer:dump">
          <LinkContainer to={Routes.SYSTEM.PROCESSBUFFERDUMP(node.node_id)}>
            <MenuItem>{t('node_actions.process_buffer_dump')}</MenuItem>
          </LinkContainer>
        </IfPermitted>
        <IfPermitted permissions="loggersmessages:read">
          <LinkContainer to={Routes.SYSTEM.SYSTEMLOGS(node.node_id)}>
            <MenuItem>{t('graylog_node_actions.recent_system_logs')}</MenuItem>
          </LinkContainer>
        </IfPermitted>
      </MoreActions>
      {showMessageProcessingModal && (
        <ConfirmDialog
          show
          onConfirm={toggleMessageProcessing}
          onCancel={() => setShowMessageProcessingModal(false)}
          title={t('graylog_node_actions.message_processing_title')}>
          <Trans
            t={t}
            i18nKey={node.is_processing ? 'graylog_node_actions.pause_confirm' : 'graylog_node_actions.resume_confirm'}
            values={{ nodeName }}
            components={{ b: <b /> }}
          />
        </ConfirmDialog>
      )}
      {loadBalancerStatusToConfirm && (
        <ConfirmDialog
          show
          onConfirm={() => updateLoadBalancerStatus(loadBalancerStatusToConfirm)}
          onCancel={() => setLoadBalancerStatusToConfirm(undefined)}
          title={t('graylog_node_actions.load_balancer_title')}>
          <Trans
            t={t}
            i18nKey="graylog_node_actions.load_balancer_confirm"
            values={{ nodeName, status: loadBalancerStatusToConfirm }}
            components={{ b: <b /> }}
          />
        </ConfirmDialog>
      )}
    </>
  );
};

export default GraylogNodeActions;
