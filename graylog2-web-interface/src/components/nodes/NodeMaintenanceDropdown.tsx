import React from 'react';
import URI from 'urijs';

import { LinkContainer } from 'components/common/router';
import { ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import { ExternalLink, IfPermitted } from 'components/common';
import Routes from 'routing/Routes';
import HideOnCloud from 'util/conditional/HideOnCloud';

type NodeMaintenanceDropdownProps = {
  node: any;
};

class NodeMaintenanceDropdown extends React.Component<NodeMaintenanceDropdownProps, {
  [key: string]: any;
}> {
  render() {
    const { node } = this.props;
    const apiBrowserURI = new URI(`${node.transport_address}/api-browser/`).normalizePathname().toString();

    return (
      <ButtonGroup>
        <DropdownButton bsStyle="info" bsSize="lg" title="Actions" id="node-maintenance-actions" pullRight>
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

          <LinkContainer to={Routes.SYSTEM.METRICS(node.node_id)}>
            <MenuItem>Metrics</MenuItem>
          </LinkContainer>

          <HideOnCloud>
            <IfPermitted permissions="loggers:read">
              <LinkContainer to={Routes.SYSTEM.LOGGING}>
                <MenuItem>Configure internal logging</MenuItem>
              </LinkContainer>
            </IfPermitted>
          </HideOnCloud>

          <MenuItem href={apiBrowserURI} target="_blank">
            <ExternalLink>API Browser</ExternalLink>
          </MenuItem>
        </DropdownButton>
      </ButtonGroup>
    );
  }
}

export default NodeMaintenanceDropdown;
