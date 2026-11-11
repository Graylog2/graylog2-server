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

import type { Stream } from 'logic/streams/types';
import useInputDetails from 'hooks/useInputDetails';
import type { ResolvedInput } from 'hooks/useInputDetails';
import { Link, Spinner } from 'components/common';
import ExpandableCellList from 'components/common/cell-list/ExpandableCellList';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';
import { useInputTitleLinkBuilder } from 'components/streams/StreamsOverview/inputTitleLinks';

type InputItem = {
  id: string;
  resolved: ResolvedInput | undefined;
  isPermitted: boolean;
};

type Props = {
  stream: Stream;
};

const AssociatedInputsCell = ({ stream }: Props) => {
  const { metrics, isInitialLoading, isError } = useStreamMetricsFor(stream.id);
  const typedInputs = metrics?.associated_inputs;
  const {
    resolvedById,
    notPermittedIds,
    isInitialLoading: areDetailsLoading,
    isFetching: areDetailsFetching,
    isError: isDetailsError,
  } = useInputDetails(typedInputs ?? []);
  const buildLink = useInputTitleLinkBuilder();

  if ((isInitialLoading && !typedInputs) || areDetailsLoading) {
    return <Spinner size="xs" />;
  }

  if (isError || !typedInputs?.length) {
    return null;
  }

  // A missing entry only means the input is deleted once the details request settled.
  const fallbackLabel = (id: string) => (areDetailsFetching || isDetailsError ? id : `${id} (deleted)`);
  const label = ({ id, resolved }: InputItem) => resolved?.title ?? fallbackLabel(id);

  const renderInput = (item: InputItem) => {
    if (!item.isPermitted) {
      return <i title="Missing permissions">{item.id}</i>;
    }

    const path = item.resolved ? buildLink(item.resolved) : null;

    return path ? <Link to={path}>{label(item)}</Link> : label(item);
  };

  // Inputs the user can't read have no title, so they go last as plain IDs.
  const items = typedInputs
    .map(({ id }) => ({ id, resolved: resolvedById[id], isPermitted: !notPermittedIds.has(id) }))
    .sort((a, b) => Number(!a.isPermitted) - Number(!b.isPermitted) || label(a).localeCompare(label(b)));

  return <ExpandableCellList items={items} renderItem={renderInput} itemKey={({ id }) => id} itemTitle={label} />;
};

export default AssociatedInputsCell;
