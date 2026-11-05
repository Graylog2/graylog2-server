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
import { useCallback } from 'react';
import * as React from 'react';
import { useTranslation } from 'react-i18next';

import { Streams } from '@graylog/server-api';

import { MenuItem } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

type Props = {
  handleFailures: (failures: Array<{ entity_id: string }>, action: 'deleted' | 'started' | 'stopped') => void;
  onSelect?: () => void;
  refetchStreams: () => void;
};

const StartStreamsActions = ({ handleFailures, refetchStreams, onSelect = undefined }: Props) => {
  const { t } = useTranslation('streams');
  const { selectedEntities } = useSelectedEntities();
  const onStartStreams = useCallback(() => {
    if (typeof onSelect === 'function') {
      onSelect();
    }

    Streams.bulkResume({ entity_ids: selectedEntities })
      .then(({ failures }) => handleFailures(failures, 'started'))
      .catch((error) => {
        UserNotification.error(t('streams_overview.bulk.start_error', { error }));
      })
      .finally(() => {
        refetchStreams();
      });
  }, [handleFailures, onSelect, refetchStreams, selectedEntities, t]);

  return (
    <MenuItem onSelect={onStartStreams}>
      {t('streams_overview.bulk.start_streams', { count: selectedEntities.length })}
    </MenuItem>
  );
};

export default StartStreamsActions;
