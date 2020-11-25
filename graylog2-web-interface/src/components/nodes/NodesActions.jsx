import PropTypes from 'prop-types';
import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import URI from 'urijs';

import { DropdownButton, DropdownSubmenu, MenuItem, Button } from 'components/graylog';
import { ExternalLinkButton, IfPermitted } from 'components/common';
import StoreProvider from 'injection/StoreProvider';
import Routes from 'routing/Routes';
import HideOnCloud from 'util/conditional/HideOnCloud';

const SystemProcessingStore = StoreProvider.getStore('SystemProcessing');
const SystemLoadBalancerStore = StoreProvider.getStore('SystemLoadBalancer');
const SystemShutdownStore = StoreProvider.getStore('SystemShutdown');

class NodesActions extends React.Component {
  static propTypes = {
    node: PropTypes.object.isRequired,
    systemOverview: PropTypes.object.isRequired,
  };

  _toggleMessageProcessing = () => {
    if (confirm(`You are about to ${this.props.systemOverview.is_processing ? 'pause' : 'resume'} message processing in this node. Are you sure?`)) {
      if (this.props.systemOverview.is_processing) {
        SystemProcessingStore.pause(this.props.node.node_id);
      } else {
        SystemProcessingStore.resume(this.props.node.node_id);
      }
    }
  };

  _changeLBStatus = (status) => {
    return () => {
      if (confirm(`You are about to change the load balancer status for this node to ${status}. Are you sure?`)) {
        SystemLoadBalancerStore.override(this.props.node.node_id, status);
      }
    };
  };

  _shutdown = () => {
    if (prompt('Do you really want to shutdown this node? Confirm by typing "SHUTDOWN".') === 'SHUTDOWN') {
      SystemShutdownStore.shutdown(this.props.node.node_id);
    }
  };

  render() {
    const apiBrowserURI = new URI(`${this.props.node.transport_address}/api-browser`).normalizePathname().toString();
    return (
      <div className="item-actions">
        <LinkContainer to={Routes.SYSTEM.NODES.SHOW(this.props.node.node_id)}>
          <Button bsStyle="info">Details</Button>
        </LinkContainer>

        <LinkContainer to={Routes.SYSTEM.METRICS(this.props.node.node_id)}>
          <Button bsStyle="info">Metrics</Button>
        </LinkContainer>

        <ExternalLinkButton bsStyle="info" href={apiBrowserURI}>
          API browser
        </ExternalLinkButton>

        <DropdownButton title="More actions" id={`more-actions-dropdown-${this.props.node.node_id}`} pullRight>
          <IfPermitted permissions="processing:changestate">
            <MenuItem onSelect={this._toggleMessageProcessing}>
              {this.props.systemOverview.is_processing ? 'Pause' : 'Resume'} message processing
            </MenuItem>
          </IfPermitted>

          <IfPermitted permissions="lbstatus:change">
            <DropdownSubmenu title="Override LB status" left>
              <MenuItem onSelect={this._changeLBStatus('ALIVE')}>ALIVE</MenuItem>
              <MenuItem onSelect={this._changeLBStatus('DEAD')}>DEAD</MenuItem>
            </DropdownSubmenu>
          </IfPermitted>

          <IfPermitted permissions="node:shutdown">
            <MenuItem onSelect={this._shutdown}>Graceful shutdown</MenuItem>
          </IfPermitted>

          <IfPermitted permissions={['processing:changestate', 'lbstatus:change', 'node:shutdown']} anyPermissions>
            <IfPermitted permissions={['inputs:read', 'threads:dump']} anyPermissions>
              <MenuItem divider />
            </IfPermitted>
          </IfPermitted>

          <HideOnCloud>
            <IfPermitted permissions="inputs:read">
              <LinkContainer to={Routes.node_inputs(this.props.node.node_id)}>
                <MenuItem>Local message inputs</MenuItem>
              </LinkContainer>
            </IfPermitted>
          </HideOnCloud>
          <IfPermitted permissions="threads:dump">
            <LinkContainer to={Routes.SYSTEM.THREADDUMP(this.props.node.node_id)}>
              <MenuItem>Get thread dump</MenuItem>
            </LinkContainer>
          </IfPermitted>
          <IfPermitted permissions="processbuffer:dump">
            <LinkContainer to={Routes.SYSTEM.PROCESSBUFFERDUMP(this.props.node.node_id)}>
              <MenuItem>Get process-buffer dump</MenuItem>
            </LinkContainer>
          </IfPermitted>
        </DropdownButton>
      </div>
    );
  }
}

export default NodesActions;
