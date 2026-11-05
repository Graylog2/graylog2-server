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
import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import numeral from 'numeral';
import moment from 'moment';
import styled from 'styled-components';
import type { TFunction } from 'i18next';

import { LinkContainer, Spinner } from 'components/common';
import { Alert, Col, Row, Button } from 'components/bootstrap';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import { DocumentationLink } from 'components/support';
import useIndexerFailuresCount from 'components/indexers/hooks/useIndexerFailuresCount';

const Header = styled.div`
  display: flex;
  justify-content: space-between;
`;

const formatTextForFailureCount = (count: number, t: TFunction) => {
  if (count === 0) {
    return t('indexers.failures.none');
  }

  return <strong>{t('indexers.failures.count', { count, formattedCount: numeral(count).format('0,0') })}</strong>;
};

const IndexerFailuresComponent = () => {
  const { t } = useTranslation('indexsets');
  const since = useMemo(() => moment().subtract(24, 'hours').format('YYYY-MM-DDTHH:mm:ss.SSS'), []);
  const { data: total, isLoading } = useIndexerFailuresCount(since);

  const failuresSummary =
    total !== undefined ? (
      <Alert bsStyle={total === 0 ? 'success' : 'danger'}>
        {formatTextForFailureCount(total, t)}

        <LinkContainer to={Routes.SYSTEM.INDICES.FAILURES}>
          <Button bsStyle="info" bsSize="xs" className="pull-right">
            {t('indexers.failures.show_errors')}
          </Button>
        </LinkContainer>
      </Alert>
    ) : null;

  return (
    <Row className="content">
      <Col md={12}>
        <Header>
          <h2>{t('indexers.failures.title')}</h2>
          <DocumentationLink page={DocsHelper.PAGES.INDEXER_FAILURES} text={t('indexers.failures.docs')} displayIcon />
        </Header>
        <p className="description">{t('indexers.failures.description')}</p>
        {isLoading ? <Spinner /> : failuresSummary}
      </Col>
    </Row>
  );
};

export default IndexerFailuresComponent;
