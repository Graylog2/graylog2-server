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
import { useQueryClient, useMutation } from '@tanstack/react-query';
import { useCallback, useState } from 'react';

import fetch from 'logic/rest/FetchProvider';
import { Button, Title, Group, Space } from 'preflight/components/common';
import Alert from 'components/bootstrap/Alert';
import URLUtils from 'util/URLUtils';
import UserNotification from 'preflight/util/UserNotification';
import useDataNodes, { DATA_NODES_OVERVIEW_QUERY_KEY } from 'preflight/hooks/useDataNodes';

const onProvisionCertificate = () => fetch(
  'POST',
  URLUtils.qualifyUrl('api/generate'),
  undefined,
  false,
);

type Props = {
  onSkipProvisioning: () => void,
}

const CertificateProvisioning = ({ onSkipProvisioning }: Props) => {
  const queryClient = useQueryClient();
  const { data: dataNodes, isInitialLoading } = useDataNodes();
  const [isProvisioning, setIsProvisioning] = useState(false);

  const { mutate: provisionCertificate } = useMutation(onProvisionCertificate, {
    onSuccess: () => {
      UserNotification.success('Started certificate provisioning successfully');
      queryClient.invalidateQueries(DATA_NODES_OVERVIEW_QUERY_KEY);
    },
    onError: (error) => {
      UserNotification.error(`Starting certificate provisioning failed with error: ${error}`);
      queryClient.invalidateQueries(DATA_NODES_OVERVIEW_QUERY_KEY);
      setIsProvisioning(false);
    },
  });

  const onSubmit = useCallback(() => {
    setIsProvisioning(true);
    provisionCertificate();
  }, [provisionCertificate]);

  return (
    <div>
      <Title order={3}>Provision certificates</Title>
      <p>
        Certificate authority has been configured successfully.<br />
        You can now provision certificate for your data nodes.
      </p>
      {(!dataNodes.length && !isInitialLoading) ? (
        <Alert bsStyle="warning">
          At least one Graylog data node needs to run before the certificate can be provisioned.
        </Alert>
      ) : <Space h="sm" />}
      <Group>
        <Button onClick={() => onSubmit()} disabled={!dataNodes.length || isProvisioning}>
          {isProvisioning ? 'Provisioning certificate...' : 'Provision certificate and continue'}
        </Button>
        <Button onClick={() => onSkipProvisioning()} variant="light" disabled={isProvisioning}>
          Skip provisioning
        </Button>
      </Group>
    </div>
  );
};

export default CertificateProvisioning;
