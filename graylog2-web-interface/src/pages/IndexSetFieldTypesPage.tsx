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
import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { DocumentTitle, PageHeader } from 'components/common';
import { Row, Col, Button } from 'components/bootstrap';
import { useStore } from 'stores/connect';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import useParams from 'routing/useParams';
import DocsHelper from 'util/DocsHelper';
import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import IndexSetFieldTypesList from 'components/indices/IndexSetFieldTypesList';
import useCurrentUser from 'hooks/useCurrentUser';

export type EditingField = {
  type: string,
  fieldName: string,
  showFiledInput: boolean,
}

const IndexSetFieldTypesPage = () => {
  const { indexSetId } = useParams();
  const navigate = useNavigate();
  const { indexSet } = useStore(IndexSetsStore);
  const currentUser = useCurrentUser();
  const [editingField, setEditingField] = useState<EditingField | null>(null);

  useEffect(() => {
    const hasMappingPermission = currentUser.permissions.includes('typemappings:edit') || currentUser.permissions.includes('*');

    if (!hasMappingPermission) {
      navigate(Routes.NOTFOUND);
    } else {
      IndexSetsActions.get(indexSetId);
    }
  }, [currentUser.permissions, indexSetId, navigate]);

  const onAddCustomFieldClick = useCallback(() => {
    setEditingField({ fieldName: undefined, showFiledInput: true, type: undefined });
  }, []);

  return (
    <DocumentTitle title={`Index Set - ${indexSet ? indexSet.title : ''}`}>
      <div>
        <PageHeader title={`Configure ${indexSet ? indexSet.title : 'Index Set'} Field Types`}
                    documentationLink={{
                      title: 'Index model documentation',
                      path: DocsHelper.PAGES.INDEX_MODEL,
                    }}
                    topActions={(
                      <LinkContainer to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSetId)}>
                        <Button bsStyle="info">Index set overview</Button>
                      </LinkContainer>
                    )}
                    actions={(
                      <Button bsStyle="success" onClick={onAddCustomFieldClick}>Change field type</Button>
                    )}>
          <span>
            Modify the current field types configuration for this index set.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <IndexSetFieldTypesList editingField={editingField} setEditingField={setEditingField} />
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default IndexSetFieldTypesPage;
