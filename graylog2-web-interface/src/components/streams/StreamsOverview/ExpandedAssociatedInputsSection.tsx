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
import useEntityTitles from 'hooks/useEntityTitles';
import { Link, Spinner } from 'components/common';
import { ListGroup, ListGroupItem } from 'components/bootstrap';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';
import Routes from 'routing/Routes';

type Props = {
  stream: Stream;
};

const ExpandedAssociatedInputsSection = ({ stream }: Props) => {
  const { metrics, isInitialLoading, isError } = useStreamMetricsFor(stream.id);
  const inputIds = metrics?.associated_inputs;

  const titleEntities = (inputIds ?? []).map((id) => ({ id, type: 'inputs' }));
  const { titlesById, isInitialLoading: areTitlesLoading } = useEntityTitles(titleEntities);

  if (isInitialLoading && !inputIds) {
    return <Spinner />;
  }

  if (isError) {
    return <p>Could not load associated inputs.</p>;
  }

  if (!inputIds || inputIds.length === 0) {
    return <p>No inputs have sent messages to this stream in the last 24 hours.</p>;
  }

  return (
    <ListGroup componentClass="ul">
      {inputIds.map((inputId) => {
        const title = titlesById[inputId];
        const label = title ?? (areTitlesLoading ? inputId : `${inputId} (deleted)`);

        return (
          <ListGroupItem key={inputId}>
            {title ? <Link to={Routes.SYSTEM.INPUT_DIAGNOSIS(inputId)}>{label}</Link> : label}
          </ListGroupItem>
        );
      })}
    </ListGroup>
  );
};

export default ExpandedAssociatedInputsSection;
