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
import React, { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Trans, useTranslation } from 'react-i18next';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import SidecarStatus from 'components/sidecars/sidecars/SidecarStatus';
import { useCollectorsAll } from 'hooks/useCollectors';
import { fetchSidecar } from 'hooks/useSidecars';
import SidecarsPageNavigation from 'components/sidecars/common/SidecarsPageNavigation';
import useParams from 'routing/useParams';
import useHistory from 'routing/useHistory';

const SidecarStatusPage = () => {
  const { t } = useTranslation('sidecars');
  const { sidecarId } = useParams<{ sidecarId: string }>();
  const history = useHistory();
  const { data: collectors } = useCollectorsAll();
  const { data: sidecar, error } = useQuery({
    queryKey: ['sidecars', 'detail', sidecarId],
    queryFn: () => fetchSidecar(sidecarId),
    refetchInterval: 5000,
  });

  useEffect(() => {
    if (error && (error as { status?: number }).status === 404) {
      history.push(Routes.SYSTEM.SIDECARS.OVERVIEW);
    }
  }, [error, history]);

  if (!sidecar || !collectors) {
    return (
      <DocumentTitle title={t('pages.status.document_title')}>
        <Spinner />
      </DocumentTitle>
    );
  }

  return (
    <DocumentTitle title={t('pages.status.document_title_named', { name: sidecar.node_name })}>
      <SidecarsPageNavigation />
      <PageHeader
        title={
          <span>
            <Trans
              t={t}
              i18nKey="pages.status.title"
              values={{ name: sidecar.node_name }}
              components={{ em: <em /> }}
            />
          </span>
        }
        documentationLink={{
          title: t('pages.status.documentation_title'),
          path: DocsHelper.PAGES.COLLECTOR_SIDECAR,
        }}>
        <span>{t('pages.status.description')}</span>
      </PageHeader>

      <SidecarStatus sidecar={sidecar} collectors={collectors} />
    </DocumentTitle>
  );
};

export default SidecarStatusPage;
