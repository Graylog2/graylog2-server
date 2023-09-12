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
import React, { useCallback, useState } from 'react';

import type { ActionComponentProps } from 'views/components/actions/ActionHandler';
import ChangeFieldTypeModal from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldTypeModal';
import usePutFiledTypeMutation from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeMutation';
import type { ChangeFieldTypeFormValues } from 'views/logic/fieldactions/ChangeFieldType/types';

const ChangeFieldType = ({
  field,
  onClose,
}: ActionComponentProps) => {
  const [show, setShow] = useState(true);

  const { putFiledTypeMutation } = usePutFiledTypeMutation();
  const handleOnClose = useCallback(() => {
    setShow(false);
    onClose();
  }, [onClose]);
  const onSubmit = useCallback(({
    indexSetSelection,
    newFieldType,
    rotated,
  }: ChangeFieldTypeFormValues) => {
    putFiledTypeMutation({
      indexSetSelection,
      newFieldType,
      rotated,
      field,
    }).then(() => handleOnClose());
  }, [field, handleOnClose, putFiledTypeMutation]);

  return show ? <ChangeFieldTypeModal onSubmit={onSubmit} field={field} onClose={handleOnClose} show={show} /> : null;
};

export default ChangeFieldType;
