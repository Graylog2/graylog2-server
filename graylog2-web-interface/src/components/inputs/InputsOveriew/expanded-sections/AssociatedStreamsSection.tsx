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

import type { InputSummary } from 'hooks/usePaginatedInputs';
import useEntityTitles from 'hooks/useEntityTitles';
import { Link, Spinner } from 'components/common';
import { ListGroup, ListGroupItem } from 'components/bootstrap';
import useInputMetrics from 'hooks/useInputMetrics';
import Routes from 'routing/Routes';

type Props = {
  input: InputSummary;
};

const AssociatedStreamsSection = ({ input }: Props) => {
  const { t } = useTranslation('inputs');
  const { metricsByInputId, isInitialLoading, isError } = useInputMetrics([input.id], ['messages_per_stream']);
  const messagesPerStream = metricsByInputId[input.id]?.messages_per_stream;

  const streamIds = messagesPerStream ? Object.keys(messagesPerStream) : [];
  const titleEntities = streamIds.map((id) => ({ id, type: 'streams' }));
  const { titlesById, isInitialLoading: areTitlesLoading } = useEntityTitles(titleEntities);

  if (isInitialLoading && !messagesPerStream) {
    return <Spinner />;
  }

  if (isError) {
    return <p>{t('overview.sections.could_not_load_associated_streams')}</p>;
  }

  if (!messagesPerStream || streamIds.length === 0) {
    return <p>{t('overview.sections.no_associated_streams')}</p>;
  }

  return (
    <ListGroup componentClass="ul">
      {streamIds.map((streamId) => {
        const title = titlesById[streamId];
        const label = title ?? (areTitlesLoading ? streamId : t('overview.sections.deleted_stream', { streamId }));

        return (
          <ListGroupItem key={streamId}>
            {title ? <Link to={Routes.stream_view(streamId)}>{label}</Link> : label}
          </ListGroupItem>
        );
      })}
    </ListGroup>
  );
};

export default AssociatedStreamsSection;
