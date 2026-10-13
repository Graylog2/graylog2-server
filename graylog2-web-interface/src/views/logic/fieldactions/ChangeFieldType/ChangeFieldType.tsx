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

import type { ActionComponentProps, ActionHandlerArguments } from 'views/components/actions/ActionHandler';
import type { AdditionalViewsActionHandlerArguments } from 'views/types';
import ChangeFieldTypeModal from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldTypeModal';
import { isFunction } from 'views/logic/aggregationbuilder/Series';
import isReservedField from 'views/logic/IsReservedField';
import useInitialSelection from 'views/logic/fieldactions/ChangeFieldType/hooks/useInitialSelection';

const ChangeFieldType = ({ field, onClose }: ActionComponentProps<AdditionalViewsActionHandlerArguments>) => {
  const [show, setShow] = useState(true);
  const handleOnClose = useCallback(() => {
    setShow(false);
    onClose();
  }, [onClose]);

  const { list, isLoading } = useInitialSelection();

  return (
    <ChangeFieldTypeModal
      initialSelectedIndexSets={list}
      onClose={handleOnClose}
      initialData={{ fieldName: field }}
      show={show}
      isLoading={isLoading}
    />
  );
};

const hasMappingPermission = ({ isPermitted }: ActionHandlerArguments['contexts']) =>
  !!isPermitted?.('typemappings:edit');

export const isChangeFieldTypeEnabled = ({ field, type, contexts }: ActionHandlerArguments) => !isFunction(field) && !type.isDecorated() && !isReservedField(field) && hasMappingPermission(contexts);

export const ChangeFieldTypeHelp = ({ contexts }: ActionHandlerArguments) => {
  if (hasMappingPermission(contexts)) return null;

  return { title: 'No permission', description: "You don't have permission to do that action" };
};

export default ChangeFieldType;
