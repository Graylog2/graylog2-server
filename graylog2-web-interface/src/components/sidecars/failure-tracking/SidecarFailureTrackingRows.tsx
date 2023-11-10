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
import styled from 'styled-components';

import { Link } from 'components/common/router';
import RelativeTime from 'components/common/RelativeTime';
import Routes from 'routing/Routes';
import StatusIndicator from 'components/sidecars/common/StatusIndicator';
import { Button } from 'components/bootstrap';

import type { Collector, SidecarSummary } from '../types';

const VerboseMessageContainer = styled.div`
  height: 80px;
  overflow: hidden scroll;
  white-space: pre-wrap;
  margin-bottom: 6px;
`;

const SecondaryText = styled.div`
  color: #94979c;
  font-style: italic;
  font-size: 66%;
`;

const ShowDetailsLink = styled(Button)`
  padding-left: 0;
`;

type Props = {
  sidecar: SidecarSummary,
  collectors: Collector[],
  onShowDetails: (obj: { name: string, verbose_message: string }) => void,
}

const SidecarFailureTrackingRows = ({ sidecar, collectors, onShowDetails }: Props) => {
  const annotation = sidecar.active ? '' : ' (inactive)';
  const {
    node_id = null,
    node_name,
    last_seen,
    node_details: {
      status: {
        message = null,
        status = undefined,
        collectors: collectorStatusList,
      } = {},
    } = {},
  } = sidecar;

  const getCollectorInformation = (collectorId: string) => collectors.find((collector) => collector.id === collectorId);

  return (

    (
      <>
        {collectorStatusList?.filter((collector) => collector.status === 2).map((collector) => {
          const collectorData = getCollectorInformation(collector.collector_id);

          return (
            <tr key={collector.collector_id + collector.configuration_id}>
              <td>
                <Link to={Routes.SYSTEM.SIDECARS.STATUS(sidecar.node_id)}>
                  {node_name}
                </Link>
                <SecondaryText>{annotation}</SecondaryText>
                <SecondaryText>{collectorData?.node_operating_system}</SecondaryText>
                <SecondaryText>v{sidecar.sidecar_version}</SecondaryText>
                <SecondaryText>{sidecar.node_id}</SecondaryText>
              </td>
              <td>
                {collectorData?.name}
              </td>
              <td>
                <RelativeTime dateTime={last_seen} />
              </td>
              <td>
                <StatusIndicator status={status}
                                 message={message}
                                 id={node_id}
                                 lastSeen={last_seen} />
              </td>
              <td>
                {collector.message}
              </td>
              <td>
                <VerboseMessageContainer>
                  {collector.verbose_message}
                </VerboseMessageContainer>
                <ShowDetailsLink bsStyle="link"
                                 bsSize="xs"
                                 onClick={() => onShowDetails({ name: collectorData?.name, verbose_message: collector.verbose_message })}>
                  Show more
                </ShowDetailsLink>
              </td>
            </tr>
          );
        })}
      </>
    )
  );
};

SidecarFailureTrackingRows.propTypes = {
  sidecar: PropTypes.object.isRequired,
};

export default SidecarFailureTrackingRows;
