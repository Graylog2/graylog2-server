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

type Props = {
  onCancel: () => void,
  onFinish: () => void,
  disableSave?: boolean,
};

const SaveOrCancelButtons = ({ onFinish, onCancel, disableSave = false }: Props) => {
  const { applyAllWidgetChanges } = useContext(WidgetEditApplyAllChangesContext);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const _onFinish = () => {
    setIsSubmitting(true);

    return applyAllWidgetChanges().then(() => {
      setIsSubmitting(false);
      onFinish();
    }).catch(() => {
      setIsSubmitting(false);
    });
  };

  return (
    <ModalSubmit submitButtonText="Apply changes"
                 submitLoadingText="Applying changes..."
                 onSubmit={_onFinish}
                 submitButtonType="button"
                 disabledSubmit={disableSave}
                 isSubmitting={isSubmitting}
                 onCancel={onCancel} />
  );
};

SaveOrCancelButtons.defaultProps = {
  disableSave: false,
};

export default SaveOrCancelButtons;
