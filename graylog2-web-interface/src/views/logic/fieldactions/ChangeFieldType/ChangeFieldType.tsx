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
import ChangeFieldTypeModal from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldTypeModal';
import { isFunction } from 'views/logic/aggregationbuilder/Series';
import type User from 'logic/users/User';
import AppConfig from 'util/AppConfig';
import isReservedField from 'views/logic/IsReservedField';
import useInitialSelection from 'views/logic/fieldactions/ChangeFieldType/hooks/useInitialSelection';
import { isPermitted } from 'util/PermissionsMixin';

const ChangeFieldType = ({
  field,
  onClose,
}: ActionComponentProps) => {
  const [show, setShow] = useState(true);
  const handleOnClose = useCallback(() => {
    setShow(false);
    onClose();
  }, [onClose]);

  const initialSelection = useInitialSelection();

  return show ? (
    <ChangeFieldTypeModal initialSelectedIndexSets={initialSelection}
                          onClose={handleOnClose}
                          initialData={{ fieldName: field }}
                          show />
  ) : null;
};

const hasMappingPermission = (currentUser: User) => isPermitted(currentUser?.permissions, 'typemappings:edit');

export const isChangeFieldTypeEnabled = ({ field, type, contexts }: ActionHandlerArguments) => {
  const { currentUser } = contexts;

  return (!isFunction(field) && !type.isDecorated() && !isReservedField(field) && hasMappingPermission(currentUser));
};

export const isChangeFieldTypeHidden = () => !AppConfig.isFeatureEnabled('field_types_management');

export const ChangeFieldTypeHelp = ({ contexts }: ActionHandlerArguments) => {
  const { currentUser } = contexts;
  hasMappingPermission(currentUser);

  if (hasMappingPermission(currentUser)) return null;

  return ({ title: 'No permission', description: 'You don\'t have permission to do that action' });
};

export default ChangeFieldType;
