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
import { Trans, useTranslation } from 'react-i18next';

import { Link, DocumentTitle, PageHeader } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import usePermissions from 'hooks/usePermissions';
import SidecarListContainer from 'components/sidecars/sidecars/SidecarListContainer';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import SidecarsPageNavigation from 'components/sidecars/common/SidecarsPageNavigation';
import useBasicSidecarUser from 'components/sidecars/hooks/useBasicSidecarUser';

const SidecarsPage = () => {
  const { t } = useTranslation('sidecars');
  const { isPermitted } = usePermissions();
  const { data: sidecarUser } = useBasicSidecarUser();
  const canCreateSidecarUserTokens = sidecarUser && isPermitted([`users:tokencreate:${sidecarUser.username}`]);

  return (
    <DocumentTitle title={t('pages.overview.document_title')}>
      <SidecarsPageNavigation />
      <PageHeader
        title={t('pages.overview.title')}
        documentationLink={{
          title: t('pages.documentation_title'),
          path: DocsHelper.PAGES.COLLECTOR_SIDECAR,
        }}>
        <span>
          {t('pages.overview.description')}
          {canCreateSidecarUserTokens && (
            <span>
              <br />
              <Trans
                t={t}
                i18nKey="pages.overview.token_hint"
                values={{ username: sidecarUser.username }}
                components={{ tokenLink: <Link to={Routes.SYSTEM.USERS.TOKENS.edit(sidecarUser.id)} />, em: <em /> }}
              />
            </span>
          )}
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <SidecarListContainer />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default SidecarsPage;
