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
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';

import { Input } from 'components/bootstrap';
import ConfirmDialog from 'components/common/ConfirmDialog';
import UserNotification from 'util/UserNotification';
import usePermissions from 'hooks/usePermissions';
import type { Stream } from 'logic/streams/types';
import useStreamMutations from 'hooks/useStreamMutations';

const StreamRuleConnector = styled.div(
  ({ theme }) => css`
    margin-top: 10px;
    margin-bottom: 13px;

    label {
      font-size: ${theme.fonts.size.small};
    }

    .form-group {
      margin-bottom: 5px;
    }

    .radio {
      margin-top: 0;
      margin-bottom: 0;
    }

    input[type='radio'] {
      margin-top: 2px;
      margin-bottom: 2px;
    }
  `,
);

type Props = {
  stream: Stream;
  onChange: () => void;
};

const MatchingTypeSwitcher = ({ stream, onChange }: Props) => {
  const { t } = useTranslation('streams');
  const [matchingType, setMatchingType] = useState<'AND' | 'OR' | undefined>(undefined);
  const { isPermitted } = usePermissions();
  const { updateStream } = useStreamMutations();
  const disabled = stream.is_default || !stream.is_editable || !isPermitted(`streams:edit:${stream.id}`);

  const handleTypeChange = (newValue: 'AND' | 'OR') => {
    // The api fn handles the error toast on rejection, so we only run the success path on resolve.
    updateStream({ streamId: stream.id, data: { matching_type: newValue } })
      .then(() => {
        onChange();

        UserNotification.success(
          newValue === 'AND' ? t('matching_type.success_all') : t('matching_type.success_any'),
          t('matching_type.success_title'),
        );

        setMatchingType(undefined);
      })
      .catch(() => {});
  };

  return (
    <StreamRuleConnector>
      <div>
        <Input
          id="streamrule-and-connector"
          type="radio"
          label={t('matching_type.all')}
          checked={stream.matching_type === 'AND'}
          onChange={() => setMatchingType('AND')}
          disabled={disabled}
        />
        <Input
          id="streamrule-or-connector"
          type="radio"
          label={t('matching_type.any')}
          checked={stream.matching_type === 'OR'}
          onChange={() => setMatchingType('OR')}
          disabled={disabled}
        />
      </div>
      {matchingType && (
        <ConfirmDialog
          show
          title={matchingType === 'AND' ? t('matching_type.switch_title_all') : t('matching_type.switch_title_any')}
          onConfirm={() => handleTypeChange(matchingType)}
          onCancel={() => setMatchingType(undefined)}>
          {t('matching_type.confirm')}
        </ConfirmDialog>
      )}
    </StreamRuleConnector>
  );
};

export default MatchingTypeSwitcher;
