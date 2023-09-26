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
import { useQuery } from '@tanstack/react-query';
import styled from 'styled-components';
import { useState } from 'react';

import { qualifyUrl } from 'util/URLUtils';
import fetch, { fetchPeriodically } from 'logic/rest/FetchProvider';
import type { DataNode } from 'preflight/types';
import UserNotification from 'util/UserNotification';
import { Spinner, Timestamp } from 'components/common';
import { Alert, Badge, ListGroup, ListGroupItem, Button } from 'components/bootstrap';
import { defaultCompare } from 'logic/DefaultCompare';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const StyledList = styled(ListGroup)`
  max-width: 700px;
  
  .list-group-item {
    display: flex;
    justify-content: space-between;
  }
`;

const DataNodeInfos = styled.div`
  display: flex;
  align-items: center;
  gap: 3px;
`;

const NodeId = styled(Badge)`
  margin-right: 3px;
`;

export const fetchDataNodes = () => fetchPeriodically<Array<DataNode>>('GET', qualifyUrl('/certrenewal'));

const useDataNodes = () => {
  const { data, isInitialLoading } = useQuery({
    queryKey: ['data-nodes', 'overview'],
    queryFn: fetchDataNodes,
    onError: (errorThrown) => {
      UserNotification.error(`Loading data nodes failed with status: ${errorThrown}`,
        'Could not load streams');
    },
    keepPreviousData: true,
    refetchInterval: 3000,

  });

  return ({
    data,
    isInitialLoading,
  });
};

const RightCol = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
`;

const renewalWording = {
  buttonTitle: 'Renew certificate',
  buttonLoadingTitle: 'Renewing certificate',
  successActionTitle: 'renewed',
  errorActionTitle: 'renewal',
  telemetryAppSection: 'renewing-certificate',
};

const provisioningWording = {
  buttonTitle: 'Provision certificate',
  buttonLoadingTitle: 'Provisioning certificate...',
  successActionTitle: 'provisioned',
  errorActionTitle: 'provisioning',
  telemetryAppSection: 'provisioning-certificate',
};

const CertRenewalButton = ({ nodeId, status }: { nodeId: string, status: DataNode['status'] }) => {
  const sendTelemetry = useSendTelemetry();
  const [isRenewing, setIsRenewing] = useState(false);
  const {
    buttonTitle,
    buttonLoadingTitle,
    successActionTitle,
    errorActionTitle,
    telemetryAppSection,
  } = status === 'UNCONFIGURED' ? provisioningWording : renewalWording;

  const onCertificateRenewal = () => {
    setIsRenewing(true);

    sendTelemetry('form_submit', {
      app_pathname: 'configurations',
      app_section: 'data-node',
      app_action_value: telemetryAppSection,
    });

    fetch('POST', qualifyUrl(`/certrenewal/${nodeId}`))
      .then(() => {
        UserNotification.success(`Certificate ${successActionTitle} successfully`);
      })
      .catch((error) => {
        UserNotification.error(`Certificate ${errorActionTitle} failed with error: ${error}`);
      })
      .finally(() => {
        setIsRenewing(false);
      });
  };

  return (
    <Button onClick={onCertificateRenewal} bsSize="xsmall">
      {isRenewing ? buttonLoadingTitle : buttonTitle}
    </Button>
  );
};

const CertificateRenewal = () => {
  const { data: dataNodes, isInitialLoading: isInitialLoadingDataNodes } = useDataNodes();
  const sortedDataNodes = dataNodes?.sort((d1, d2) => defaultCompare(d1.cert_valid_until, d2.cert_valid_until));

  return (
    <div>
      <h2>Certificate Renewal & Provisioning</h2>
      <p>
        Here you can manually trigger the certificate renewal or provisioning for Graylog data nodes.
        It is only necessary to manually provision certificates when the renewal policy mode &quot;Manual&quot; is configured and
        data nodes have been started after the initial certificate provisioning.
      </p>

      {!!sortedDataNodes?.length && (
        <StyledList>
            {sortedDataNodes.map(({
              node_id,
              hostname,
              transport_address,
              short_node_id,
              cert_valid_until,
              status,
            }) => (
              <ListGroupItem key={short_node_id}>
                <DataNodeInfos>
                  <NodeId title="Short node id" bsStyle="primary">{short_node_id}</NodeId>
                  <span title="Transport address">{transport_address}</span>{' – '}
                  <span title="Hostname">{hostname}</span>
                </DataNodeInfos>
                <RightCol>
                  {cert_valid_until && (<span>valid until <Timestamp dateTime={cert_valid_until} />{' '}</span>)}
                  <CertRenewalButton nodeId={node_id} status={status} />
                </RightCol>
              </ListGroupItem>
            ))}
        </StyledList>
      )}

      {isInitialLoadingDataNodes && <Spinner />}
      {(!sortedDataNodes?.length && !isInitialLoadingDataNodes) && (
        <Alert>
          No data nodes have been found.
        </Alert>
      )}
    </div>
  );
};

export default CertificateRenewal;
