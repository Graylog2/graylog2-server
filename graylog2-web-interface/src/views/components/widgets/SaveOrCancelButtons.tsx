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
import { useCallback } from 'react';
import { useFormikContext } from 'formik';

import { Button, ButtonToolbar } from 'components/graylog';

type Props = {
  onCancel: () => void,
  onFinish: (...args: any[]) => void,
};

const SaveOrCancelButtons = ({ onFinish, onCancel }: Props) => {
  const { handleSubmit, dirty } = useFormikContext();
  const _onFinish = useCallback((...args) => {
    if (handleSubmit && dirty) {
      handleSubmit();
    }

    return onFinish(...args);
  }, [onFinish, handleSubmit, dirty]);

  return (
    <ButtonToolbar className="pull-right">
      <Button onClick={_onFinish} bsStyle="primary">Save</Button>
      <Button onClick={onCancel}>Cancel</Button>
    </ButtonToolbar>
  );
};

export default SaveOrCancelButtons;
