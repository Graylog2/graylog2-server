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

import { ConfirmDialog } from 'components/common';

type Props = {
  show: boolean;
  streamTitle?: string;
  onConfirm: () => void;
  onCancel: () => void;
  isSubmitting?: boolean;
};

const StartStreamAfterRuleCreateDialog = ({
  show,
  streamTitle = '',
  onConfirm,
  onCancel,
  isSubmitting = false,
}: Props) => (
  <ConfirmDialog
    show={show}
    title="Start stream?"
    btnConfirmText="Start Stream"
    submitLoadingText="Starting Stream..."
    isAsyncSubmit
    isSubmitting={isSubmitting}
    onConfirm={onConfirm}
    onCancel={onCancel}>
    Stream Rules on this stream are currently paused and will not take effect.
    {streamTitle ? (
      <>
        {' '}
        Would you like to start stream <strong>{streamTitle}</strong>?
      </>
    ) : (
      ' Would you like to start this stream?'
    )}
  </ConfirmDialog>
);

export default StartStreamAfterRuleCreateDialog;
