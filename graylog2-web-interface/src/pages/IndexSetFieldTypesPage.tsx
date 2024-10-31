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
import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { Row, Col } from 'components/bootstrap';
import { useStore } from 'stores/connect';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import useParams from 'routing/useParams';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import IndexSetFieldTypesList from 'components/indices/IndexSetFieldTypes/IndexSetFieldTypesList';
import ChangeFieldTypeButton from 'components/indices/IndexSetFieldTypes/ChangeFieldTypeButton';
import useHasTypeMappingPermission from 'hooks/useHasTypeMappingPermission';
import { IndicesPageNavigation } from 'components/indices';
import isIndexFieldTypeChangeAllowed from 'components/indices/helpers/isIndexFieldTypeChangeAllowed';

const IndexSetFieldTypesPage = () => {
  const { indexSetId } = useParams();
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();
  const { indexSet } = useStore(IndexSetsStore);
  const hasMappingPermission = useHasTypeMappingPermission();

  useEffect(() => {
    if (!hasMappingPermission) {
      navigate(Routes.NOTFOUND);
    } else {
      IndexSetsActions.get(indexSetId).then(() => setIsLoading(false));
    }
  }, [hasMappingPermission, indexSetId, navigate]);

  const indexFieldTypeChangeAllowed = useMemo(() => isIndexFieldTypeChangeAllowed(indexSet), [indexSet]);

  return (
    <DocumentTitle title={`Index Set - ${indexSet ? indexSet.title : ''}`}>
      <IndicesPageNavigation />
      <PageHeader title={`Configure ${indexSet ? indexSet.title : 'Index Set'} Field Types`}
                  documentationLink={{
                    title: 'Index model documentation',
                    path: DocsHelper.PAGES.INDEX_MODEL,
                  }}
                  actions={indexFieldTypeChangeAllowed && <ChangeFieldTypeButton indexSetId={indexSetId} />}>
        <span>
          The data represents field types from 2 last indices and the fields with custom field type. You can modify the current field types configuration for this index set.
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          {isLoading ? <Spinner /> : <IndexSetFieldTypesList />}
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default IndexSetFieldTypesPage;
