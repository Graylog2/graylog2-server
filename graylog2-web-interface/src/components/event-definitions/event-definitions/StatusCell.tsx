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
import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';

import {
  enableEventDefinition,
  disableEventDefinition,
  EVENT_DEFINITIONS_QUERY_KEY,
} from 'components/event-definitions/hooks/useEventDefinitions';
import { Badge, BootstrapModalConfirm } from 'components/bootstrap';
import usePermissions from 'hooks/usePermissions';

import type { EventDefinition } from '../event-definitions-types';
import { isSystemEventDefinition } from '../event-definitions-types';

const _title = (
  disabled: boolean,
  disabledChange: boolean,
  description: string,
  enableText: string,
  disableText: string,
) => {
  if (disabledChange) {
    return description;
  }

  return disabled ? enableText : disableText;
};

type Props = {
  eventDefinition: EventDefinition;
};

const StatusCell = ({ eventDefinition }: Props) => {
  const { t } = useTranslation('event_definitions');
  const [showConfirmDisableModal, setShowConfirmDisableModal] = useState<boolean>(false);
  const queryClient = useQueryClient();
  const isEnabled = eventDefinition?.state === 'ENABLED';
  const { isPermitted } = usePermissions();
  const disableChange =
    isSystemEventDefinition(eventDefinition) || !isPermitted(`eventdefinitions:edit:${eventDefinition.id}`);
  const description = isEnabled ? t('status_cell.enabled') : t('status_cell.disabled');
  const title = _title(!isEnabled, disableChange, description, t('status_cell.enable'), t('status_cell.disable'));
  const toggleIcon = isEnabled ? 'pause' : 'play_arrow';

  const toggleEventDefinitionStatus = useCallback(async () => {
    if (isEnabled) {
      setShowConfirmDisableModal(true);
    } else {
      await enableEventDefinition(eventDefinition);
      await queryClient.invalidateQueries({ queryKey: EVENT_DEFINITIONS_QUERY_KEY });
    }
  }, [isEnabled, eventDefinition, queryClient]);

  const handleConfirmDisable = useCallback(async () => {
    await disableEventDefinition(eventDefinition);
    await queryClient.invalidateQueries({ queryKey: EVENT_DEFINITIONS_QUERY_KEY });
    setShowConfirmDisableModal(false);
  }, [eventDefinition, queryClient]);

  return (
    <>
      <Badge
        color={isEnabled ? 'success' : 'warning'}
        variant="light"
        onClick={disableChange ? undefined : toggleEventDefinitionStatus}
        title={title}
        aria-label={title}
        rightIcon={disableChange ? undefined : toggleIcon}>
        {description}
      </Badge>
      {showConfirmDisableModal && (
        <BootstrapModalConfirm
          showModal
          title={t('status_cell.disable_title')}
          onConfirm={handleConfirmDisable}
          onCancel={() => setShowConfirmDisableModal(false)}>
          {t('status_cell.disable_confirm', { title: eventDefinition.title })}
        </BootstrapModalConfirm>
      )}
    </>
  );
};

export default StatusCell;
