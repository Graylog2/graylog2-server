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

import { Alert } from 'components/bootstrap';
import NameOnlyEntityManager, { type NameOnlyTelemetryEvent } from 'components/common/NameOnlyEntityManager';
import { Spinner } from 'components/common';
import {
  useEventDefinitionTags,
  useEventDefinitionTagMutations,
} from 'components/event-definitions/hooks/useTags';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';

const Tags = () => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const { tags, loadingTags, tagsLoadError } = useEventDefinitionTags();
  const { addTag, addingTag, updateTag, updatingTag, deleteTag, deletingTag } =
    useEventDefinitionTagMutations();

  const handleTelemetry = (event: NameOnlyTelemetryEvent) => {
    const payload = { app_pathname: getPathnameWithoutId(pathname), app_section: 'event-definitions-config' };
    switch (event) {
      case 'added':
        sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_TAG_ADDED, payload);
        break;
      case 'updated':
        sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_TAG_UPDATED, payload);
        break;
      case 'deleted':
        sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_TAG_DELETED, payload);
        break;
      default:
        break;
    }
  };

  if (loadingTags) {
    return <Spinner text="Loading tags..." />;
  }

  if (tagsLoadError) {
    return (
      <Alert bsStyle="danger" title="Could not load tags">
        Failed to fetch event definition tags. Please try again.
      </Alert>
    );
  }

  return (
    <NameOnlyEntityManager
      title="Manage Tags"
      entityLabel="tag"
      items={tags}
      onAdd={(value) => addTag(value)}
      onUpdate={(id, value) => updateTag({ id, value })}
      onDelete={(id) => deleteTag(id)}
      busy={{ adding: addingTag, updating: updatingTag, deleting: deletingTag }}
      onTelemetry={handleTelemetry}
    />
  );
};

export default Tags;
