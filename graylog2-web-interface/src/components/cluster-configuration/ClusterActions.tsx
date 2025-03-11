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
import URI from 'urijs';

import { LinkContainer } from 'components/common/router';
import { ConfirmDialog, ExternalLink, IfPermitted } from 'components/common';
import { DropdownButton, MenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { SystemLoadBalancerStore } from 'stores/load-balancer/SystemLoadBalancerStore';
import { SystemProcessingStore } from 'stores/system-processing/SystemProcessingStore';

import type { GraylogNode } from './useClusterNodes';

type Props = {
  node: GraylogNode;
};

const ClusterActions = ({ node }: Props) => {
  const [showMessageProcessingModal, setShowMessageProcessingModal] = useState<boolean>(false);
  const [loadBalancerStatusToConfirm, setLoadBalancerStatusToConfirm] = useState<'ALIVE' | 'DEAD' | undefined>(
    undefined,
  );

  const apiBrowserURI = new URI(`${node.transport_address}/api-browser/`).normalizePathname().toString();
  const nodeName = `${node.short_node_id} / ${node.hostname}`;

  const toggleMessageProcessing = () => {
    if (node.is_processing) {
      SystemProcessingStore.pause(node.node_id);
    } else {
      SystemProcessingStore.resume(node.node_id);
    }
    setShowMessageProcessingModal(false);
  };

  const updateLoadBalancerStatus = (status: 'ALIVE' | 'DEAD') => {
    SystemLoadBalancerStore.override(node.node_id, status);
    setLoadBalancerStatusToConfirm(undefined);
  };

  return (
    <>
      <DropdownButton bsSize="xs" title="More" id={`more-actions-dropdown-${node.node_id}`} pullRight>
        <IfPermitted permissions="processing:changestate">
          <MenuItem onSelect={() => setShowMessageProcessingModal(true)}>
            {node.is_processing ? 'Pause' : 'Resume'} message processing
          </MenuItem>
        </IfPermitted>
        <IfPermitted permissions="lbstatus:change">
          {node.lb_status === 'alive' ? (
            <MenuItem onSelect={() => setLoadBalancerStatusToConfirm('DEAD')}>
              Override load Balancer status to DEAD
            </MenuItem>
          ) : (
            <MenuItem onSelect={() => setLoadBalancerStatusToConfirm('ALIVE')}>
              Override load Balancer status to ALIVE
            </MenuItem>
          )}
        </IfPermitted>
        <IfPermitted permissions={['processing:changestate', 'lbstatus:change', 'node:shutdown']} anyPermissions>
          <IfPermitted permissions={['inputs:read', 'threads:dump']} anyPermissions>
            <MenuItem divider />
          </IfPermitted>
        </IfPermitted>
        <LinkContainer to={Routes.SYSTEM.METRICS(node.node_id)}>
          <MenuItem>Metrics</MenuItem>
        </LinkContainer>
        <HideOnCloud>
          <IfPermitted permissions="inputs:read">
            <LinkContainer to={Routes.node_inputs(node.node_id)}>
              <MenuItem>Local message inputs</MenuItem>
            </LinkContainer>
          </IfPermitted>
        </HideOnCloud>
        <IfPermitted permissions="threads:dump">
          <LinkContainer to={Routes.SYSTEM.THREADDUMP(node.node_id)}>
            <MenuItem>Get thread dump</MenuItem>
          </LinkContainer>
        </IfPermitted>
        <IfPermitted permissions="processbuffer:dump">
          <LinkContainer to={Routes.SYSTEM.PROCESSBUFFERDUMP(node.node_id)}>
            <MenuItem>Get process-buffer dump</MenuItem>
          </LinkContainer>
        </IfPermitted>
        <IfPermitted permissions="loggersmessages:read">
          <LinkContainer to={Routes.SYSTEM.SYSTEMLOGS(node.node_id)}>
            <MenuItem>Get recent system log messages</MenuItem>
          </LinkContainer>
        </IfPermitted>
        <MenuItem href={apiBrowserURI} target="_blank">
          <ExternalLink>API Browser</ExternalLink>
        </MenuItem>
      </DropdownButton>
      {showMessageProcessingModal && (
        <ConfirmDialog
          show
          onConfirm={toggleMessageProcessing}
          onCancel={() => setShowMessageProcessingModal(false)}
          title="Message Processing">
          <>
            You are about to <b>{node.is_processing ? 'pause' : 'resume'}</b> message processing in <b>{nodeName}</b>{' '}
            node. Are you sure?
          </>
        </ConfirmDialog>
      )}
      {loadBalancerStatusToConfirm && (
        <ConfirmDialog
          show
          onConfirm={() => updateLoadBalancerStatus(loadBalancerStatusToConfirm)}
          onCancel={() => setLoadBalancerStatusToConfirm(undefined)}
          title="Load Balancer">
          <>
            You are about to change the load balancer status for <b>{nodeName}</b> node to{' '}
            <b>{loadBalancerStatusToConfirm}</b>. Are you sure?
          </>
        </ConfirmDialog>
      )}
    </>
  );
};

export default ClusterActions;
