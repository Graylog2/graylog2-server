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
import * as React from 'react';
import PropTypes from 'prop-types';

import { Link } from 'components/common/router';
import RelativeTime from 'components/common/RelativeTime';
import Routes from 'routing/Routes';
import StatusIndicator from 'components/sidecars/common/StatusIndicator';
import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';

import type { SidecarSummary } from '../types';

type Props = {
  sidecar: SidecarSummary,
}

const SidecarFailureTrackingRows = ({ sidecar }: Props) => {
  //const annotation = sidecar.active ? '' : ' (inactive)';
  let sidecarStatus = { status: null, message: null, id: null };

  if (sidecar.node_details.status && SidecarStatusEnum.isValidStatusCode(sidecar.node_details.status.status)) {
    sidecarStatus = {
      status: sidecar.node_details.status.status,
      message: sidecar.node_details.status.message,
      id: sidecar.node_id,
    };
  }

  const renderSidecarCollectorRows = () => {
    return sidecar.node_details.status.collectors.filter((collector) => collector.status === 2).map((collector) => (
      <tr key={collector.collector_id + collector.configuration_id}>
        <td>
          {sidecar.active
            ? (
              <Link to={Routes.SYSTEM.SIDECARS.STATUS(sidecar.node_id)}>
                {sidecar.node_name}
              </Link>
            )
            : sidecar.node_name}
        </td>
        <td>
          <RelativeTime dateTime={sidecar.last_seen} />
        </td>
        <td>
          <StatusIndicator status={sidecarStatus.status}
                           message={sidecarStatus.message}
                           id={sidecarStatus.id}
                           lastSeen={sidecar.last_seen} />
        </td>
        <td>
          {collector.message}
        </td>
        <td>
          {collector.verbose_message}
        </td>
      </tr>
    ));
  };

  return (
    <>
      {renderSidecarCollectorRows()}
    </>
  );
};

SidecarFailureTrackingRows.propTypes = {
  sidecar: PropTypes.object.isRequired,
};

export default SidecarFailureTrackingRows;
