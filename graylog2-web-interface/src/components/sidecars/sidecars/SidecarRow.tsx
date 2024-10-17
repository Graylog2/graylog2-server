import React from 'react';
import styled, { css } from 'styled-components';

import { Link, LinkContainer } from 'components/common/router';
import { Button, ButtonToolbar } from 'components/bootstrap';
import { RelativeTime, Timestamp } from 'components/common';
import Routes from 'routing/Routes';
import OperatingSystemIcon from 'components/sidecars/common/OperatingSystemIcon';
import StatusIndicator from 'components/sidecars/common/StatusIndicator';
import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';
import recentMessagesTimeRange from 'util/TimeRangeHelper';

import style from './SidecarRow.css';

const SidecarTR = styled.tr<{ inactive: boolean }>(({ inactive, theme }) => css`
  color: ${inactive ? theme.utils.contrastingColor(theme.colors.global.contentBackground, 'AA') : 'currentColor'};
  opacity: ${inactive ? 0.9 : 1};

  td:not(:last-child) {
    font-style: ${inactive ? 'italic' : 'normal'};
  }
`);

type SidecarRowProps = {
  sidecar: any;
};

class SidecarRow extends React.Component<SidecarRowProps, {
  [key: string]: any;
}> {
  constructor(props) {
    super(props);

    this.state = {
      showRelativeTime: true,
    };
  }

  render() {
    const { showRelativeTime } = this.state;
    const { sidecar } = this.props;
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
      <SidecarTR inactive={!sidecar.active}>
        <td className={style.sidecarName}>
          <Link to={Routes.SYSTEM.SIDECARS.STATUS(sidecar.node_id)}>
            {sidecar.node_name}
          </Link>
        </td>
        <td aria-label="Status">
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
          {showRelativeTime
            ? <RelativeTime dateTime={sidecar.last_seen} />
            : <Timestamp dateTime={sidecar.last_seen} />}
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
            <LinkContainer to={Routes.search_with_query(`gl2_source_collector:${sidecar.node_id}`, 'absolute', recentMessagesTimeRange())}>
              <Button bsSize="xsmall" bsStyle="info">Show messages</Button>
            </LinkContainer>
          </ButtonToolbar>
        </td>
      </SidecarTR>
    );
  }
}

export default SidecarRow;
