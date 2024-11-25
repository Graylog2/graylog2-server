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
import React from 'react';
import styled, { css } from 'styled-components';

import type { IndexInfo } from 'stores/indices/IndicesStore';
import { Row, Col } from 'components/bootstrap';
import type { IndexSummary as IndexSummaryType } from 'stores/indexers/IndexerOverviewStore';
import { ClosedIndexDetails, IndexDetails, IndexSummary } from 'components/indices';
import NumberUtils from 'util/NumberUtils';

const Index = ({ index, indexDetails, indexSetId }: { index: IndexSummaryType, indexDetails: Array<IndexInfo>, indexSetId: string }) => {
  const indexRange = index && index.range ? index.range : null;
  const details = indexDetails.find(({ index_name }) => index_name === index.index_name);

  return (
    <Row className="content index-description">
      <Col md={12}>
        <IndexSummary index={index}
                      name={index.index_name}
                      indexRange={indexRange}
                      isDeflector={index.is_deflector}>
          <span>
            <IndexDetails index={details}
                          indexName={index.index_name}
                          indexRange={indexRange}
                          indexSetId={indexSetId}
                          isDeflector={index.is_deflector} />
          </span>
        </IndexSummary>
      </Col>
    </Row>
  );
};

const ClosedIndex = ({ index }: { index: IndexSummaryType }) => {
  const indexRange = index.range;

  return (
    <Row className="content index-description">
      <Col md={12}>
        <IndexSummary index={index} name={index.index_name} indexRange={indexRange} isDeflector={index.is_deflector}>
          <span>
            <ClosedIndexDetails indexName={index.index_name} indexRange={indexRange} />
          </span>
        </IndexSummary>
      </Col>
    </Row>
  );
};

const IndexListItem = ({ indexDetails, index, indexSetId } : {indexDetails: Array<IndexInfo>, index: IndexSummaryType, indexSetId: string}) => (
  !index.is_closed
    ? <Index index={index} indexDetails={indexDetails} indexSetId={indexSetId} key={`index-summary-${index.index_name}`} />
    : <ClosedIndex index={index} key={`index-summary-${index.index_name}`} />
);

type Props = {
  headline: string,
  subheading: string,
  indices: Array<IndexSummaryType>,
  indexDetails: Array<IndexInfo>,
  indexSetId: string
}

const SectionHeader = styled(Row)(({ theme }) => css`
  margin-top: ${theme.spacings.lg};
  margin-bottom: ${theme.spacings.lg};
`);

const SectionHeadline = styled.h2(({ theme }) => css`
  margin-bottom: ${theme.spacings.xs};
`);

const SectionSubheading = styled.p`
  margin-bottom: 0;
`;

const StatList = styled.dl`
  margin-bottom: 0;

  dt {
    float: left;
    width: 190px;
    overflow: hidden;
    clear: left;
    text-align: left;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  dd {
    margin-left: 180px;
  }
`;

const IndexSection = ({ headline, subheading, indices, indexDetails, indexSetId }: Props) => {
  const size = indices.map((index) => index.size?.bytes || 0).reduce((partialSum, a) => partialSum + a, 0) || 0;
  const shards = indices.map((index) => index.shard_count).reduce((partialSum, a) => partialSum + a, 0) || 0;

  return (
    <>
      <SectionHeader>
        <Col md={6}>
          <SectionHeadline>{headline}</SectionHeadline>
          <SectionSubheading>{subheading}</SectionSubheading>
        </Col>
        <Col md={6}>
          <StatList>
            <dt>Indices:</dt>
            <dd>{indices.length}</dd>
            <dt>Shards:</dt>
            <dd>{shards}</dd>
            <dt>Total Size:</dt>
            <dd>{NumberUtils.formatBytes(size)}</dd>
          </StatList>
        </Col>
      </SectionHeader>
      {indices.map((index) => <IndexListItem indexDetails={indexDetails} index={index} indexSetId={indexSetId} />)}
    </>
  );
};

export default IndexSection;
