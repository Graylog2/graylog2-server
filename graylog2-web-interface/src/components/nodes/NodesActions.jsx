import React, {PropTypes} from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, DropdownButton, MenuItem } from 'react-bootstrap';

import { IfPermitted } from 'components/common';

import SystemProcessingStore from 'stores/system-processing/SystemProcessingStore';
import SystemLoadBalancerStore from 'stores/load-balancer/SystemLoadBalancerStore';

import Routes from 'routing/Routes';

const NodesActions = React.createClass({
  propTypes: {
    node: PropTypes.object.isRequired,
    systemOverview: PropTypes.object.isRequired,
  },
  _toggleMessageProcessing() {
    if (confirm(`You are about to ${this.props.systemOverview.is_processing ? 'pause' : 'resume'} message processing in this node. Are you sure?`)) {
      if (this.props.systemOverview.is_processing) {
        SystemProcessingStore.pause(this.props.node.node_id);
      } else {
        SystemProcessingStore.resume(this.props.node.node_id);
      }
    }
  },
  _changeLBStatus(status) {
    return () => {
      if (confirm(`You are about to change the load balancer status for this node to ${status}. Are you sure?`)) {
        SystemLoadBalancerStore.override(this.props.node.node_id, status);
      }
    };
  },
  render() {
    return (
      <div className="item-actions">
        <LinkContainer to={Routes.SYSTEM.NODES.SHOW(this.props.node.node_id)}>
          <Button bsStyle="info">Details</Button>
        </LinkContainer>

        <LinkContainer to={Routes.SYSTEM.METRICS(this.props.node.node_id)}>
          <Button bsStyle="info">Metrics</Button>
        </LinkContainer>

        <Button bsStyle="info" href={`${this.props.node.transport_address}api-browser`} target="_blank">
          <i className="fa fa-external-link"/>&nbsp; API browser
        </Button>

        <DropdownButton title="More actions" id={`more-actions-dropdown-${this.props.node.node_id}`} pullRight>
          <IfPermitted permissions="processing:changestate">
            <MenuItem onClick={this._toggleMessageProcessing}>
              {this.props.systemOverview.is_processing ? 'Pause' : 'Resume'} message processing
            </MenuItem>
          </IfPermitted>

          <IfPermitted permissions="lbstatus:change">
            <li className="dropdown-submenu left-submenu">
              <a href="#">Override LB status</a>
              <ul className="dropdown-menu">
                <MenuItem onClick={this._changeLBStatus('ALIVE')}>ALIVE</MenuItem>
                <MenuItem onClick={this._changeLBStatus('DEAD')}>DEAD</MenuItem>
              </ul>
            </li>
          </IfPermitted>

          <IfPermitted permissions="node:shutdown">
            <MenuItem>Graceful shutdown</MenuItem>
          </IfPermitted>

          <IfPermitted permissions={['processing:changestate', 'lbstatus:change', 'node:shutdown']} anyPermissions>
            <IfPermitted permissions={['inputs:read', 'threads:dump']} anyPermissions>
              <MenuItem divider/>
            </IfPermitted>
          </IfPermitted>

          <IfPermitted permissions="inputs:read">
            <MenuItem>Local message inputs</MenuItem>
          </IfPermitted>
          <IfPermitted permissions="threads:dump">
            <MenuItem>Get thread dump</MenuItem>
          </IfPermitted>
        </DropdownButton>
      </div>
    );
  },
});

export default NodesActions;
