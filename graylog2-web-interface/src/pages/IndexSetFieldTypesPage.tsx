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

import { DocumentTitle, PageHeader } from 'components/common';
import { Row, Button } from 'components/bootstrap';
import { useStore } from 'stores/connect';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import useParams from 'routing/useParams';
import DocsHelper from 'util/DocsHelper';
import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import IndexSetFieldTypesList from 'components/indices/IndexSetFieldTypesList';

const IndexSetFieldTypesPage = () => {
  const { indexSetId } = useParams();
  const { indexSet } = useStore(IndexSetsStore);

  useEffect(() => {
    IndexSetsActions.get(indexSetId);
  }, [indexSetId]);

  return (
    <DocumentTitle title={`Index Set - ${indexSet ? indexSet.title : ''}`}>
      <div>
        <PageHeader title="Configure Index Set Field Types"
                    documentationLink={{
                      title: 'Index model documentation',
                      path: DocsHelper.PAGES.INDEX_MODEL,
                    }}
                    topActions={(
                      <LinkContainer to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSetId)}>
                        <Button bsStyle="info">Index set overview</Button>
                      </LinkContainer>
                    )}>
          <span>
            Modify the current field types configuration for this index set.
          </span>
        </PageHeader>

        <Row className="content">
          <IndexSetFieldTypesList />
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default IndexSetFieldTypesPage;
