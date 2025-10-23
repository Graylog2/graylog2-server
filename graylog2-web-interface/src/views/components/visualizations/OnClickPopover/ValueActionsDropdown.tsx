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

import type { FieldData, Step } from 'views/components/visualizations/OnClickPopover/Types';
import useCurrentQueryId from 'views/logic/queries/useCurrentQueryId';
import { ActionContext } from 'views/logic/ActionContext';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import ActionDropdown from 'views/components/actions/ActionDropdown';
import TypeSpecificValue from 'views/components/TypeSpecificValue';
import useOverflowingComponents from 'views/hooks/useOverflowingComponents';
import { Menu } from 'components/bootstrap';
import Popover from 'components/common/Popover';
import hasMultipleValueForActions from 'views/components/visualizations/utils/hasMultipleValueForActions';
import { humanSeparator } from 'views/Constants';
import PopoverTitle from 'views/components/visualizations/OnClickPopover/PopoverTitle';

type Props = {
  onActionRun: () => void;
  value: FieldData['value'];
  field: FieldData['field'];
  setStep: React.Dispatch<React.SetStateAction<Step>>;
};

const ValueActionsDropdown = ({ value, field, onActionRun, setStep }: Props) => {
  const queryId = useCurrentQueryId();
  const actionContext = useContext(ActionContext);
  const { overflowingComponents, setOverflowingComponents } = useOverflowingComponents();

  const handlerArgs = useMemo(() => {
    const type = fieldTypeFor(field, actionContext.fieldTypes);

    return { queryId, field, type, value, contexts: actionContext };
  }, [actionContext, field, queryId, value]);

  const showMultipleValueHeader = hasMultipleValueForActions(actionContext);

  const onBackToValueSelect = () => setStep('values');

  return (
    <Popover.Dropdown
      title={
        <PopoverTitle onBackClick={onBackToValueSelect}>
          {showMultipleValueHeader ? (
            actionContext?.valuePath.map((o) => Object.values(o)[0]).join(humanSeparator)
          ) : (
            <>
              {field} = <TypeSpecificValue field={field} value={value} type={handlerArgs?.type} truncate />
            </>
          )}
        </PopoverTitle>
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

export default ValueActionsDropdown;
