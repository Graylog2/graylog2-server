import React, { PropTypes } from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { ButtonGroup, DropdownButton, MenuItem } from 'react-bootstrap';
import URI from 'urijs';

import { IfPermitted } from 'components/common';

import Routes from 'routing/Routes';

const NodeMaintenanceDropdown = React.createClass({
  propTypes: {
    node: PropTypes.object.isRequired,
  },
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

          <LinkContainer to={Routes.SYSTEM.METRICS(this.props.node.node_id)}>
            <MenuItem>Metrics</MenuItem>
          </LinkContainer>

          <IfPermitted permissions="loggers:read">
            <LinkContainer to={Routes.SYSTEM.LOGGING}>
              <MenuItem>Configure internal logging</MenuItem>
            </LinkContainer>
          </IfPermitted>

          <MenuItem href={apiBrowserURI} target="_blank">
            API Browser <i className="fa fa-external-link" />
          </MenuItem>
        </DropdownButton>
      </ButtonGroup>
    );
  },
});

export default NodeMaintenanceDropdown;
