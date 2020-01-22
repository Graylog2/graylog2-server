import PropTypes from 'prop-types';
import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { ButtonGroup, DropdownButton, MenuItem } from 'components/graylog';
import URI from 'urijs';

import { ExternalLink, IfPermitted } from 'components/common';

import Routes from 'routing/Routes';

class NodeMaintenanceDropdown extends React.Component {
  static propTypes = {
    node: PropTypes.object.isRequired,
  };

  render() {
    const apiBrowserURI = new URI(`${this.props.node.transport_address}/api-browser`).normalizePathname().toString();
    return (
      <ButtonGroup>
        <DropdownButton bsStyle="info" bsSize="lg" title="Actions" id="node-maintenance-actions" pullRight>
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

          <LinkContainer to={Routes.SYSTEM.METRICS(this.props.node.node_id)}>
            <MenuItem>Metrics</MenuItem>
          </LinkContainer>

          <IfPermitted permissions="loggers:read">
            <LinkContainer to={Routes.SYSTEM.LOGGING}>
              <MenuItem>Configure internal logging</MenuItem>
            </LinkContainer>
          </IfPermitted>

          <MenuItem href={apiBrowserURI} target="_blank">
            <ExternalLink>API Browser</ExternalLink>
          </MenuItem>
        </DropdownButton>
      </ButtonGroup>
    );
  }
}

export default NodeMaintenanceDropdown;
