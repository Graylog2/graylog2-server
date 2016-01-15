import React, {PropTypes} from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, DropdownButton, MenuItem } from 'react-bootstrap';

import { IfPermitted } from 'components/common';

import Routes from 'routing/Routes';

const NodesActions = React.createClass({
  propTypes: {
    node: PropTypes.object.isRequired,
    systemOverview: PropTypes.object.isRequired,
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
            {this.props.systemOverview.is_processing ? <MenuItem>Pause message processing</MenuItem> :
              <MenuItem>Resume message processing</MenuItem>}
          </IfPermitted>

          <IfPermitted permissions="lbstatus:change">
            <li className="dropdown-submenu left-submenu">
              <a href="#">Override LB status</a>
              <ul className="dropdown-menu">
                {this.props.systemOverview.lb_status !== 'alive' && <MenuItem>ALIVE</MenuItem>}
                {this.props.systemOverview.lb_status !== 'dead' && <MenuItem>DEAD</MenuItem>}
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
