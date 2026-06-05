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
      <DocumentTitle title="Sidecar status">
        <Spinner />
      </DocumentTitle>
    );
  }

  return (
    <DocumentTitle title={`Sidecar ${sidecar.node_name} status`}>
      <SidecarsPageNavigation />
      <PageHeader
        title={
          <span>
            Sidecar <em>{sidecar.node_name} status</em>
          </span>
        }
        documentationLink={{
          title: 'Sidecars documentation',
          path: DocsHelper.PAGES.COLLECTOR_SIDECAR,
        }}>
        <span>A status overview of the Sidecar.</span>
      </PageHeader>

      <SidecarStatus sidecar={sidecar} collectors={collectors} />
    </DocumentTitle>
  );
};

export default SidecarStatusPage;
