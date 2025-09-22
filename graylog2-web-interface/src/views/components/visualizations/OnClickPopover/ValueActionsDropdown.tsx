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
import React, { useContext, useMemo } from 'react';

import type { FieldData } from 'views/components/visualizations/OnClickPopover/Types';
import useCurrentQueryId from 'views/logic/queries/useCurrentQueryId';
import { ActionContext } from 'views/logic/ActionContext';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import ActionDropdown from 'views/components/actions/ActionDropdown';
import TypeSpecificValue from 'views/components/TypeSpecificValue';
import useOverflowingComponents from 'views/hooks/useOverflowingComponents';
import { Menu } from 'components/bootstrap';
import Popover from 'components/common/Popover';

const useQueryFieldTypes = () => {
  const fieldTypes = useContext(FieldTypesContext);

  return useMemo(() => fieldTypes.currentQuery, [fieldTypes.currentQuery]);
};

const ValueActionsDropdown = ({ value, field, onActionRun }: FieldData & { onActionRun: () => void }) => {
  const queryId = useCurrentQueryId();
  const actionContext = useContext(ActionContext);
  const types = useQueryFieldTypes();
  const { overflowingComponents, setOverflowingComponents } = useOverflowingComponents();

  const handlerArgs = useMemo(() => {
    const type = fieldTypeFor(field, types);

    return { queryId, field, type, value, contexts: actionContext };
  }, [actionContext, field, queryId, types, value]);

  return (
    <Popover.Dropdown>
      <Menu opened>
        <ActionDropdown
          handlerArgs={handlerArgs}
          type="value"
          onMenuToggle={onActionRun}
          overflowingComponents={overflowingComponents}
          setOverflowingComponents={setOverflowingComponents}>
          {field} = <TypeSpecificValue field={field} value={value} type={handlerArgs?.type} truncate />
        </ActionDropdown>
      </Menu>
    </Popover.Dropdown>
  );
};

export default ValueActionsDropdown;
