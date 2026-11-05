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
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';

import { ConfirmDialog, IconButton } from 'components/common';
import type { Output } from 'hooks/useOutputs';
import useStreamOutputMutation from 'hooks/useStreamOutputMutations';
import { keyFn } from 'hooks/useStreamOutputs';

type Props = {
  output: Output;
  streamId: string;
};

const RemoveOutputButton = ({ output, streamId }: Props) => {
  const { t } = useTranslation('streams');
  const [showConfirmRemove, setShowConfirmRemove] = useState(false);
  const { removeStreamOutput } = useStreamOutputMutation();
  const queryClient = useQueryClient();

  const onConfirmRemoveOutput = () => {
    removeStreamOutput({ streamId, outputId: output.id }).then(() =>
      queryClient.invalidateQueries({ queryKey: keyFn(streamId) }),
    );
  };

  return (
    <>
      <IconButton
        name="delete"
        iconType="regular"
        title={t('routing_destination.remove_output.title')}
        bsStyle="danger"
        onClick={() => setShowConfirmRemove(true)}
      />
      <ConfirmDialog
        show={showConfirmRemove}
        onConfirm={onConfirmRemoveOutput}
        onCancel={() => setShowConfirmRemove(false)}
        title={t('routing_destination.remove_output.title')}>
        <p>{t('routing_destination.remove_output.confirm')}</p>
      </ConfirmDialog>
    </>
  );
};

export default RemoveOutputButton;
