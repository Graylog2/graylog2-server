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
import React, { useCallback, useMemo } from 'react';
import numeral from 'numeral';
import moment from 'moment';

import { Col, Row } from 'components/bootstrap';
import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, Spinner, PageHeader, PaginatedList } from 'components/common';
import { IndexerFailuresList } from 'components/indexers';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import useIndexerFailuresCount from 'components/indexers/hooks/useIndexerFailuresCount';
import useIndexerFailuresList from 'components/indexers/hooks/useIndexerFailuresList';

const IndexerFailuresPage = () => {
  const { page, pageSize, setPagination } = usePaginationQueryParameter();
  const since = useMemo(() => moment().subtract(10, 'years').format('YYYY-MM-DDTHH:mm:ss.SSS'), []);
  const { data: total, isLoading: isLoadingCount } = useIndexerFailuresCount(since);
  const offset = (page - 1) * pageSize;
  const { data: listData, isLoading: isLoadingList } = useIndexerFailuresList(pageSize, offset);

  const onChangePaginatedList = useCallback(
    (newPage: number, newSize: number) => {
      setPagination({ page: newPage, pageSize: newSize });
    },
    [setPagination],
  );

  if (isLoadingCount || isLoadingList) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title="Indexer failures">
      <span>
        <PageHeader
          title="Indexer failures"
          documentationLink={{
            title: 'Indexer failures documentation',
            path: DocsHelper.PAGES.INDEXER_FAILURES,
          }}>
          <span>
            This is a list of message index attempts that failed. A failure means that a message was properly processed
            but writing it to the indexer cluster failed. Note that the list is capped to a size of 50 MB so it will
            contain a lot of failure logs but not necessarily all that ever occurred.
            <br />
            Collection containing a total of {numeral(total).format('0,0')} indexer failures.
          </span>
        </PageHeader>
        <Row className="content">
          <Col md={12}>
            <PaginatedList totalItems={total} onChange={onChangePaginatedList}>
              <IndexerFailuresList failures={(listData as any)?.failures} />
            </PaginatedList>
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

export default IndexerFailuresPage;
