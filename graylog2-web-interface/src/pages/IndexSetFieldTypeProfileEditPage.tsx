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
import { useTranslation } from 'react-i18next';

import useHistory from 'routing/useHistory';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import { Col, Row } from 'components/bootstrap';
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import useParams from 'routing/useParams';
import EditProfile from 'components/indices/IndexSetFieldTypeProfiles/EditProfile';
import useHasTypeMappingPermission from 'hooks/useHasTypeMappingPermission';
import { IndicesPageNavigation } from 'components/indices';

const IndexSetFieldTypeProfileEditPage = () => {
  const { t } = useTranslation('indexsets');
  const { profileId } = useParams();
  const { data, isFetching } = useProfile(profileId);
  const { push } = useHistory();
  const hasMappingPermission = useHasTypeMappingPermission();

  useEffect(() => {
    if (!hasMappingPermission) {
      push(Routes.NOTFOUND);
    }
  }, [hasMappingPermission, push]);

  return (
    <DocumentTitle title={t('pages.profile_edit_document_title')}>
      <IndicesPageNavigation />
      <PageHeader
        title={t('pages.profile_edit_title', { name: data?.name })}
        documentationLink={{
          title: t('overview.header.documentation_link'),
          path: DocsHelper.PAGES.INDEX_MODEL,
        }}>
        <span>{t('pages.profile_edit_description', { name: data?.name })}</span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>{!isFetching ? <EditProfile profile={data} /> : <Spinner />}</Col>
      </Row>
    </DocumentTitle>
  );
};

export default IndexSetFieldTypeProfileEditPage;
