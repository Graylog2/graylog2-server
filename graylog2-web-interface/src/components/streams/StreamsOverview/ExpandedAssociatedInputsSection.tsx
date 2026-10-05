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
import { Link, Spinner } from 'components/common';
import { ListGroup, ListGroupItem } from 'components/bootstrap';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';
import { useInputTitleLinkBuilder } from 'components/streams/StreamsOverview/inputTitleLinks';

type Props = {
  stream: Stream;
};

const ExpandedAssociatedInputsSection = ({ stream }: Props) => {
  const { metrics, isInitialLoading, isError } = useStreamMetricsFor(stream.id);
  const typedInputs = metrics?.associated_inputs;

  const { resolvedById, isFetching: areDetailsFetching, isError: isDetailsError } = useInputDetails(typedInputs ?? []);
  const buildLink = useInputTitleLinkBuilder();

  if (isInitialLoading && !typedInputs) {
    return <Spinner />;
  }

  if (isError) {
    return <p>Could not load associated inputs.</p>;
  }

  if (!typedInputs || typedInputs.length === 0) {
    return <p>No inputs have sent messages to this stream in the last 24 hours.</p>;
  }

  return (
    <ListGroup componentClass="ul">
      {typedInputs.map(({ id: inputId }) => {
        const resolved = resolvedById[inputId];
        const label = resolved?.title ?? (areDetailsFetching || isDetailsError ? inputId : `${inputId} (deleted)`);
        const path = resolved ? buildLink(resolved) : null;

        return <ListGroupItem key={inputId}>{path ? <Link to={path}>{label}</Link> : label}</ListGroupItem>;
      })}
    </ListGroup>
  );
};

export default ExpandedAssociatedInputsSection;
