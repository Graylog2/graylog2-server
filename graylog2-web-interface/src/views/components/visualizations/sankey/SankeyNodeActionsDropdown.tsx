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
import React, { useContext } from 'react';

import { ActionContext } from 'views/logic/ActionContext';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import ActionDropdown from 'views/components/actions/ActionDropdown';
import TypeSpecificValue from 'views/components/TypeSpecificValue';
import useOverflowingComponents from 'views/hooks/useOverflowingComponents';
import { Menu } from 'components/bootstrap';
import Popover from 'components/common/Popover';
import useFieldActions from 'views/components/actions/useFieldActions';

type Props = {
  field: string;
  value: unknown;
  onActionRun: () => void;
};

const SankeyNodeActionsDropdown = ({ field, value, onActionRun }: Props) => {
  const actionContext = useContext(ActionContext);
  const { additionalHandlerArgs } = useFieldActions();
  const { overflowingComponents, setOverflowingComponents } = useOverflowingComponents();
  const type = fieldTypeFor(field, actionContext.fieldTypes);
  const handlerArgs = {
    field,
    type,
    value,
    contexts: actionContext,
    ...additionalHandlerArgs,
  };

  return (
    <Popover.Dropdown
      title={
        <>
          {field} = <TypeSpecificValue field={field} value={value} type={type} truncate />
        </>
      }>
      <Menu opened>
        <ActionDropdown
          handlerArgs={handlerArgs}
          type="value"
          onMenuToggle={onActionRun}
          overflowingComponents={overflowingComponents}
          setOverflowingComponents={setOverflowingComponents}
        />
      </Menu>
    </Popover.Dropdown>
  );
};

export default SankeyNodeActionsDropdown;
