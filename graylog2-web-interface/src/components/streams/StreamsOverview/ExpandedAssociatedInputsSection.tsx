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
import { useTranslation } from 'react-i18next';

import type { Stream } from 'logic/streams/types';
import useInputDetails from 'hooks/useInputDetails';
import { Link, Spinner } from 'components/common';
import { ListGroup, ListGroupItem } from 'components/bootstrap';
import useStreamMetrics from 'hooks/useStreamMetrics';
import { useInputTitleLinkBuilder } from 'components/streams/StreamsOverview/inputTitleLinks';

type Props = {
  stream: Stream;
};

const ExpandedAssociatedInputsSection = ({ stream }: Props) => {
  const { t } = useTranslation('streams');
  const { metricsByStreamId, isInitialLoading, isError } = useStreamMetrics([stream.id], ['associated_inputs']);
  const typedInputs = metricsByStreamId[stream.id]?.associated_inputs;

  const { resolvedById, isFetching: areDetailsFetching, isError: isDetailsError } = useInputDetails(typedInputs ?? []);
  const buildLink = useInputTitleLinkBuilder();

  if (isInitialLoading && !typedInputs) {
    return <Spinner />;
  }

  if (isError) {
    return <p>{t('streams_overview.expanded.associated_inputs_load_failed')}</p>;
  }

  if (!typedInputs || typedInputs.length === 0) {
    return <p>{t('streams_overview.expanded.associated_inputs_none')}</p>;
  }

  return (
    <ListGroup componentClass="ul">
      {typedInputs.map(({ id: inputId }) => {
        const resolved = resolvedById[inputId];
        const label =
          resolved?.title ??
          (areDetailsFetching || isDetailsError ? inputId : t('streams_overview.expanded.deleted', { id: inputId }));
        const path = resolved ? buildLink(resolved) : null;

        return <ListGroupItem key={inputId}>{path ? <Link to={path}>{label}</Link> : label}</ListGroupItem>;
      })}
    </ListGroup>
  );
};

export default ExpandedAssociatedInputsSection;
