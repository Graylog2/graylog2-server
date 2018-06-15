import PropTypes from 'prop-types';
import React from 'react';
import { Button, ButtonToolbar } from 'react-bootstrap';
import { Link } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { Timestamp } from 'components/common';
import OperatingSystemIcon from 'components/sidecars/common/OperatingSystemIcon';
import StatusIndicator from 'components/sidecars/common/StatusIndicator';

import style from './SidecarRow.css';

class SidecarRow extends React.Component {
  static propTypes = {
    sidecar: PropTypes.object.isRequired,
  };

  state = {
    showRelativeTime: true,
  };

  render() {
    const sidecar = this.props.sidecar;
    const sidecarClass = sidecar.active ? '' : style.greyedOut;
    const annotation = sidecar.active ? '' : ' (inactive)';
    let sidecarStatus = null;
    if (sidecar.node_details.status) {
      sidecarStatus = sidecar.node_details.status.status;
    }
    return (
      <tr className={sidecarClass}>
        <td className={style.sidecarName}>
          {sidecar.active ?
            <Link to={Routes.SYSTEM.SIDECARS.STATUS(sidecar.node_id)}>
              {sidecar.node_name}
            </Link> :
            sidecar.node_name
          }
        </td>
        <td>
          <StatusIndicator status={sidecarStatus} />
        </td>
        <td>
          <OperatingSystemIcon operatingSystem={sidecar.node_details.operating_system} />&ensp;
          {sidecar.node_details.operating_system}
        </td>
        <td>
          <Timestamp dateTime={sidecar.last_seen} relative={this.state.showRelativeTime} />
        </td>
        <td>
          {sidecar.node_id}
          {annotation}
        </td>
        <td>
          {sidecar.sidecar_version}
        </td>
        <td>
          <ButtonToolbar>
            <LinkContainer to={`${Routes.SYSTEM.SIDECARS.ADMINISTRATION}?node_id=${sidecar.node_id}`}>
              <Button bsSize="xsmall" bsStyle="info">Manage sidecar</Button>
            </LinkContainer>
            <LinkContainer to={Routes.search_with_query(`gl2_source_collector:${sidecar.node_id}`, 'relative', 604800)}>
              <Button bsSize="xsmall" bsStyle="info">Show messages</Button>
            </LinkContainer>
          </ButtonToolbar>
        </td>
      </tr>
    );
  }
}

export default SidecarRow;
