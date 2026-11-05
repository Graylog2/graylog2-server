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
import React, { useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';

import useHistory from 'routing/useHistory';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { Row, Col } from 'components/bootstrap';
import useSingleIndexSet from 'components/indices/hooks/useSingleIndexSet';
import useParams from 'routing/useParams';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import IndexSetFieldTypesList from 'components/indices/IndexSetFieldTypes/IndexSetFieldTypesList';
import ChangeFieldTypeButton from 'components/indices/IndexSetFieldTypes/ChangeFieldTypeButton';
import useHasTypeMappingPermission from 'hooks/useHasTypeMappingPermission';
import { IndicesPageNavigation } from 'components/indices';
import isIndexFieldTypeChangeAllowed from 'components/indices/helpers/isIndexFieldTypeChangeAllowed';

const IndexSetFieldTypesPage = () => {
  const { t } = useTranslation('indexsets');
  const { indexSetId } = useParams();
  const { push } = useHistory();
  const { data: indexSet, isInitialLoading } = useSingleIndexSet(indexSetId);
  const hasMappingPermission = useHasTypeMappingPermission();

  useEffect(() => {
    if (!hasMappingPermission) {
      push(Routes.NOTFOUND);
    }
  }, [hasMappingPermission, push]);

  const indexFieldTypeChangeAllowed = useMemo(() => isIndexFieldTypeChangeAllowed(indexSet), [indexSet]);

  return (
    <DocumentTitle title={t('overview.document_title', { title: indexSet ? indexSet.title : '' })}>
      <IndicesPageNavigation />
      <PageHeader
        title={t('pages.field_types_title', { title: indexSet ? indexSet.title : t('pages.index_set_fallback') })}
        documentationLink={{
          title: t('overview.header.documentation_link'),
          path: DocsHelper.PAGES.INDEX_MODEL,
        }}
        actions={indexFieldTypeChangeAllowed && <ChangeFieldTypeButton indexSetId={indexSetId} />}>
        <span>{t('pages.field_types_description')}</span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>{isInitialLoading ? <Spinner /> : <IndexSetFieldTypesList />}</Col>
      </Row>
    </DocumentTitle>
  );
};

export default IndexSetFieldTypesPage;
