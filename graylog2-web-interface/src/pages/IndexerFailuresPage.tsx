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
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation('indexsets');
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
    <DocumentTitle title={t('pages.failures_title')}>
      <span>
        <PageHeader
          title={t('pages.failures_title')}
          documentationLink={{
            title: t('indexers.failures.docs'),
            path: DocsHelper.PAGES.INDEXER_FAILURES,
          }}>
          <span>
            {t('pages.failures_description')}
            <br />
            {t('pages.failures_total', { count: total, formattedCount: numeral(total).format('0,0') })}
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
