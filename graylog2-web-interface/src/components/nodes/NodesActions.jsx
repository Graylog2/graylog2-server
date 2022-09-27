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
import PropTypes from 'prop-types';
import React from 'react';
import URI from 'urijs';

import { LinkContainer } from 'components/common/router';
import { DropdownSubmenu, ExternalLinkButton, IfPermitted } from 'components/common';
import { Button, DropdownButton, MenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { SystemLoadBalancerStore } from 'stores/load-balancer/SystemLoadBalancerStore';
import { SystemProcessingStore } from 'stores/system-processing/SystemProcessingStore';

class NodesActions extends React.Component {
  static propTypes = {
    node: PropTypes.object.isRequired,
    systemOverview: PropTypes.object.isRequired,
  };

  _toggleMessageProcessing = () => {
    const { systemOverview, node } = this.props;

    // eslint-disable-next-line no-alert
    if (window.confirm(`You are about to ${systemOverview.is_processing ? 'pause' : 'resume'} message processing in this node. Are you sure?`)) {
      if (systemOverview.is_processing) {
        SystemProcessingStore.pause(node.node_id);
      } else {
        SystemProcessingStore.resume(node.node_id);
      }
    }
  };

  _changeLBStatus = (status) => {
    return () => {
      // eslint-disable-next-line no-alert
      if (window.confirm(`You are about to change the load balancer status for this node to ${status}. Are you sure?`)) {
        const { node } = this.props;
        SystemLoadBalancerStore.override(node.node_id, status);
      }
    };
  };

  render() {
    const { systemOverview, node } = this.props;
    const apiBrowserURI = new URI(`${node.transport_address}/api-browser/`).normalizePathname().toString();

    return (
      <div className="item-actions">
        <LinkContainer to={Routes.SYSTEM.NODES.SHOW(node.node_id)}>
          <Button>Details</Button>
        </LinkContainer>

        <LinkContainer to={Routes.SYSTEM.METRICS(node.node_id)}>
          <Button>Metrics</Button>
        </LinkContainer>

        <ExternalLinkButton href={apiBrowserURI}>
          API browser
        </ExternalLinkButton>

        <DropdownButton title="More actions" id={`more-actions-dropdown-${node.node_id}`} pullRight>
          <IfPermitted permissions="processing:changestate">
            <MenuItem onSelect={this._toggleMessageProcessing}>
              {systemOverview.is_processing ? 'Pause' : 'Resume'} message processing
            </MenuItem>
          </IfPermitted>

          <IfPermitted permissions="lbstatus:change">
            <DropdownSubmenu title="Override LB status" left>
              <MenuItem onSelect={this._changeLBStatus('ALIVE')}>ALIVE</MenuItem>
              <MenuItem onSelect={this._changeLBStatus('DEAD')}>DEAD</MenuItem>
            </DropdownSubmenu>
          </IfPermitted>

          <IfPermitted permissions={['processing:changestate', 'lbstatus:change', 'node:shutdown']} anyPermissions>
            <IfPermitted permissions={['inputs:read', 'threads:dump']} anyPermissions>
              <MenuItem divider />
            </IfPermitted>
          </IfPermitted>

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
        </DropdownButton>
      </div>
    );
  }
}

export default NodesActions;
