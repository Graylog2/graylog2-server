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
import { useTranslation } from 'react-i18next';

import useProductName from 'brand-customization/useProductName';
import fetch from 'logic/rest/FetchProvider';
import { Title, Group, Space } from 'preflight/components/common';
import Alert from 'components/bootstrap/Alert';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import useDataNodes, { DATA_NODES_OVERVIEW_QUERY_KEY } from 'preflight/hooks/useDataNodes';
import Button from 'components/bootstrap/Button';

const onProvisionCertificate = () => fetch('POST', URLUtils.qualifyUrl('api/generate'), undefined, false);

type Props = {
  onSkipProvisioning: () => void;
};

const CertificateProvisioning = ({ onSkipProvisioning }: Props) => {
  const { t } = useTranslation('preflight');
  const productName = useProductName();
  const queryClient = useQueryClient();
  const { data: dataNodes, isInitialLoading } = useDataNodes();
  const [isProvisioning, setIsProvisioning] = useState(false);

  const { mutate: provisionCertificate } = useMutation({
    mutationFn: onProvisionCertificate,

    onSuccess: () => {
      UserNotification.success(t('certificate_provisioning.started'));
      queryClient.invalidateQueries({ queryKey: DATA_NODES_OVERVIEW_QUERY_KEY });
    },

    onError: (error) => {
      UserNotification.error(t('certificate_provisioning.error', { error: String(error) }));
      queryClient.invalidateQueries({ queryKey: DATA_NODES_OVERVIEW_QUERY_KEY });
      setIsProvisioning(false);
    },
  });

  const onSubmit = useCallback(() => {
    setIsProvisioning(true);
    provisionCertificate();
  }, [provisionCertificate]);

  return (
    <div>
      <Title order={3}>{t('certificate_provisioning.title')}</Title>
      <p>
        {t('certificate_provisioning.configured')}
        <br />
        {t('certificate_provisioning.ready')}
      </p>
      {!dataNodes.length && !isInitialLoading ? (
        <Alert bsStyle="warning">{t('certificate_provisioning.data_node_required', { productName })}</Alert>
      ) : (
        <Space h="sm" />
      )}
      <Group>
        <Button bsStyle="info" onClick={() => onSubmit()} disabled={!dataNodes.length || isProvisioning}>
          {isProvisioning ? t('certificate_provisioning.provisioning') : t('certificate_provisioning.provision')}
        </Button>
        <Button onClick={() => onSkipProvisioning()} disabled={isProvisioning}>
          {t('certificate_provisioning.skip')}
        </Button>
      </Group>
    </div>
  );
};

export default CertificateProvisioning;
