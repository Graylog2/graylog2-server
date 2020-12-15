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
import styled, { css } from 'styled-components';

import { Link, LinkContainer } from 'components/graylog/router';
import { Button, ButtonToolbar } from 'components/graylog';
import Routes from 'routing/Routes';
import { Timestamp } from 'components/common';
import OperatingSystemIcon from 'components/sidecars/common/OperatingSystemIcon';
import StatusIndicator from 'components/sidecars/common/StatusIndicator';
import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';

import style from './SidecarRow.css';

const SidecarTR = styled.tr(({ inactive, theme }) => css`
  color: ${inactive ? theme.utils.contrastingColor(theme.colors.table.background, 'AA') : 'currentColor'};
  opacity: ${inactive ? 0.9 : 1};

  &:nth-of-type(2n+1) {
    color: ${inactive ? theme.utils.contrastingColor(theme.colors.table.backgroundAlt, 'AA') : 'currentColor'};
  }

  td:not(:last-child) {
    font-style: ${inactive ? 'italic' : 'normal'};
  }
`);

class SidecarRow extends React.Component {
  static propTypes = {
    sidecar: PropTypes.object.isRequired,
  };

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
          <Timestamp dateTime={sidecar.last_seen} relative={showRelativeTime} />
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
      </SidecarTR>
    );
  }
}

export default SidecarRow;
