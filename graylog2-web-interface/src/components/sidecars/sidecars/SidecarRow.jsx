import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';

import { Button, ButtonToolbar } from 'components/graylog';
import Routes from 'routing/Routes';
import { Timestamp } from 'components/common';
import OperatingSystemIcon from 'components/sidecars/common/OperatingSystemIcon';
import StatusIndicator from 'components/sidecars/common/StatusIndicator';
import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';

import commonStyle from 'components/sidecars/common/CommonSidecarStyles.css';
import style from './SidecarRow.css';

class SidecarRow extends React.Component {
  static propTypes = {
    sidecar: PropTypes.object.isRequired,
  };

  state = {
    showRelativeTime: true,
  };

  render() {
    const { sidecar } = this.props;
    const sidecarClass = sidecar.active ? '' : commonStyle.greyedOut;
    const annotation = sidecar.active ? '' : ' (inactive)';
    let sidecarStatus = { status: null, message: null, id: null };

    if (sidecar.node_details.status && SidecarStatusEnum.isValidStatusCode(sidecar.node_details.status.status)) {
      sidecarStatus = {
        status: sidecar.node_details.status.status,
        message: sidecar.node_details.status.message,
        id: sidecar.node_id,
      };
    }
    return (
      <tr className={sidecarClass}>
        <td className={style.sidecarName}>
          {sidecar.active
            ? (
              <Link to={Routes.SYSTEM.SIDECARS.STATUS(sidecar.node_id)}>
                {sidecar.node_name}
              </Link>
            )
            : sidecar.node_name}
        </td>
        <td>
          <StatusIndicator status={sidecarStatus.status}
                           message={sidecarStatus.message}
                           id={sidecarStatus.id}
                           lastSeen={sidecar.last_seen} />
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
