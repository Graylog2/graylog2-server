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
import styled, { css } from 'styled-components';

import SectionGrid from 'components/common/Section/SectionGrid';
import IndicesConfiguration from 'components/indices/IndicesConfiguration';
import HideOnCloud from 'util/conditional/HideOnCloud';

import FieldTypeProfileCell from '../cells/FieldTypeProfileCell';
import type { IndexSetEntity } from '../types';

const Container = styled(SectionGrid)(
  ({ theme }) => css`
    row-gap: ${theme.spacings.md};

    dl {
      display: grid;
      grid-template-columns: max-content minmax(0, 1fr);
      gap: ${theme.spacings.xxs} ${theme.spacings.lg};
      margin: 0;
    }

    dt {
      color: ${theme.colors.text.secondary};
      font-weight: normal;
    }

    dd {
      margin: 0;
      overflow-wrap: anywhere;
    }
  `,
);

const GroupTitle = styled.strong(
  ({ theme }) => css`
    display: block;
    margin-bottom: ${theme.spacings.xs};
  `,
);

type Props = {
  indexSet: IndexSetEntity;
};

const IndexSetDetailsSection = ({ indexSet }: Props) => (
  <Container $columns="minmax(0, 1fr) minmax(0, 2fr)">
    <div>
      <GroupTitle>Index set</GroupTitle>
      <dl>
        <dt>Index prefix:</dt>
        <dd>{indexSet.index_prefix}</dd>
        <HideOnCloud>
          <dt>Shards:</dt>
          <dd>{indexSet.shards}</dd>
          <dt>Replicas:</dt>
          <dd>{indexSet.replicas}</dd>
        </HideOnCloud>
        <dt>Field type refresh interval:</dt>
        <dd>{indexSet.field_type_refresh_interval / 1000.0} seconds</dd>
        <dt>Field type profile:</dt>
        <dd>
          <FieldTypeProfileCell profileId={indexSet.field_type_profile} />
        </dd>
      </dl>
    </div>
    <div>
      <GroupTitle>Rotation & retention</GroupTitle>
      <IndicesConfiguration indexSet={indexSet} />
    </div>
  </Container>
);

export default IndexSetDetailsSection;
