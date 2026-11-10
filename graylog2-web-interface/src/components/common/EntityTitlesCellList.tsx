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

type EntityItem = {
  id: string;
  title: string | undefined;
  isPermitted: boolean;
};

// Entities the user can't read have no title, so they go last as plain IDs.
const compareEntities = (a: EntityItem, b: EntityItem) =>
  Number(!a.isPermitted) - Number(!b.isPermitted) || (a.title ?? a.id).localeCompare(b.title ?? b.id);

type Props = {
  entityIds: Array<string>;
  // Entity type of the system catalog, e.g. `streams`.
  entityType: string;
  entityRoute: (id: string) => string;
};

/**
 * Lists entities by their titles in an entity data table cell, linked to their pages.
 */
const EntityTitlesCellList = ({ entityIds, entityType, entityRoute }: Props) => {
  const { titlesById, notPermittedIds, isInitialLoading, isFetching, isError } = useEntityTitles(
    entityIds.map((id) => ({ id, type: entityType })),
  );

  if (isInitialLoading) {
    return <Spinner size="xs" />;
  }

  const items = entityIds
    .map((id) => ({ id, title: titlesById[id], isPermitted: !notPermittedIds.has(id) }))
    .sort(compareEntities);

  const renderEntity = ({ id, title, isPermitted }: EntityItem) => {
    if (!isPermitted) {
      return <i title="Missing permissions">{id}</i>;
    }

    if (title) {
      return <Link to={entityRoute(id)}>{title}</Link>;
    }

    // A missing title only means the entity is deleted once the titles request settled.
    return isFetching || isError ? id : `${id} (deleted)`;
  };

  return (
    <ExpandableCellList
      items={items}
      renderItem={renderEntity}
      itemKey={({ id }) => id}
      itemTitle={({ id, title }) => title ?? id}
    />
  );
};

export default EntityTitlesCellList;
