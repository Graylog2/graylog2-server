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

import useEntityTitles from 'hooks/useEntityTitles';
import { Link, Spinner } from 'components/common';
import ExpandableCellList from 'components/common/ExpandableCellList';
import Routes from 'routing/Routes';

const PIPELINE_ENTITY_TYPE = 'pipeline_processor_pipelines';

type PipelineItem = {
  id: string;
  title: string | undefined;
  isPermitted: boolean;
};

const renderPipeline = ({ id, title, isPermitted }: PipelineItem) => {
  if (!isPermitted) {
    return <i title="Missing permissions">{id}</i>;
  }

  return title ? <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(id)}>{title}</Link> : `${id} (deleted)`;
};

// Pipelines the user can't read have no title, so they go last as plain IDs.
const comparePipelines = (a: PipelineItem, b: PipelineItem) =>
  Number(!a.isPermitted) - Number(!b.isPermitted) || (a.title ?? a.id).localeCompare(b.title ?? b.id);

type Props = {
  pipelineIds: Array<string>;
};

const PipelinesList = ({ pipelineIds }: Props) => {
  const { titlesById, notPermittedIds, isInitialLoading } = useEntityTitles(
    pipelineIds.map((id) => ({ id, type: PIPELINE_ENTITY_TYPE })),
  );

  if (isInitialLoading) {
    return <Spinner size="xs" />;
  }

  const items = pipelineIds
    .map((id) => ({ id, title: titlesById[id], isPermitted: !notPermittedIds.has(id) }))
    .sort(comparePipelines);

  return (
    <ExpandableCellList
      items={items}
      renderItem={renderPipeline}
      itemKey={({ id }) => id}
      itemTitle={({ id, title }) => title ?? id}
    />
  );
};

export default PipelinesList;
