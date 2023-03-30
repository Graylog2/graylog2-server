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
import { useContext, useState } from 'react';

import WidgetEditApplyAllChangesContext from 'views/components/contexts/WidgetEditApplyAllChangesContext';
import { ModalSubmit } from 'components/common';
import DisableSubmissionStateContext from 'views/components/contexts/DisableSubmissionStateContext';

type Props = {
  onCancel: () => void,
  onSubmit: () => void,
};

const SaveOrCancelButtons = ({ onSubmit, onCancel }: Props) => {
  const { applyAllWidgetChanges } = useContext(WidgetEditApplyAllChangesContext);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { disabled: disabledSubmit } = useContext(DisableSubmissionStateContext);

  const _onSubmit = () => {
    setIsSubmitting(true);

    return applyAllWidgetChanges().then(() => {
      setIsSubmitting(false);
      onSubmit();
    }).catch(() => {
      setIsSubmitting(false);
    });
  };

  return (
    <ModalSubmit isAsyncSubmit
                 submitButtonText="Update widget"
                 submitLoadingText="Updating widget..."
                 onSubmit={_onSubmit}
                 submitButtonType="button"
                 disabledSubmit={disabledSubmit}
                 isSubmitting={isSubmitting}
                 displayCancel
                 onCancel={onCancel} />
  );
};

export default SaveOrCancelButtons;
