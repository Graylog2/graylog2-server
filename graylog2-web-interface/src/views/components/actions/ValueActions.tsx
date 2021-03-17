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
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { MenuItem } from 'components/graylog';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { ActionContext } from 'views/logic/ActionContext';
import type { QueryId } from 'views/logic/queries/Query';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

import { createHandlerFor } from './ActionHandler';
import type { ActionDefinition } from './ActionHandler';

import OverlayDropdown from '../OverlayDropdown';
import style from '../Value.css';
import CustomPropTypes from '../CustomPropTypes';

type Props = {
  children: React.ReactNode,
  element: React.ReactNode,
  field: string,
  menuContainer: HTMLElement | undefined | null,
  queryId: QueryId,
  type: FieldType,
  value: React.ReactNode,
};

const ValueActions = ({ children, element, field, menuContainer, queryId, type, value }: Props) => {
  const actionContext = useContext(ActionContext);
  const { unsetWidgetFocusing } = useContext(WidgetFocusContext);
  const [open, setOpen] = useState(false);
  const [overflowingComponents, setOverflowingComponents] = useState({});

  const _onMenuToggle = () => setOpen(!open);
  const overflowingComponentsValues: Array<React.ReactNode> = Object.values(overflowingComponents);
  const handlerArgs = { queryId, field, type, value, contexts: actionContext };
  const valueActions = PluginStore.exports('valueActions')
    .filter((action: ActionDefinition) => {
      const { isHidden = () => false } = action;

      return !isHidden(handlerArgs);
    })
    .map((action: ActionDefinition) => {
      const setActionComponents = (fn) => {
        setOverflowingComponents(fn(overflowingComponents));
      };

      const handler = createHandlerFor(action, setActionComponents);

      const onSelect = () => {
        const { resetFocus } = action;

        if (resetFocus) {
          unsetWidgetFocusing();
        }

        _onMenuToggle();
        handler(handlerArgs);
      };

      const { isEnabled = () => true } = action;
      const actionDisabled = !isEnabled(handlerArgs);

      return (
        <MenuItem key={`value-action-${field}-${action.type}`}
                  disabled={actionDisabled}
                  eventKey={{ field, value }}
                  onSelect={onSelect}>{action.title}
        </MenuItem>
      );
    });

  return (
    <>
      <OverlayDropdown show={open}
                       toggle={element}
                       placement="right"
                       onToggle={_onMenuToggle}
                       menuContainer={menuContainer}>
        <li className={style.bottomSpacer}>
          <span className={style.dropdownheader}>
            {children}
          </span>
        </li>

        <MenuItem divider />
        <MenuItem header>Actions</MenuItem>
        {valueActions}
      </OverlayDropdown>
      {overflowingComponentsValues}
    </>
  );
};

ValueActions.propTypes = {
  children: PropTypes.node.isRequired,
  element: PropTypes.node.isRequired,
  field: PropTypes.string.isRequired,
  menuContainer: PropTypes.object,
  queryId: PropTypes.string.isRequired,
  type: CustomPropTypes.FieldType,
  value: PropTypes.node.isRequired,
};

ValueActions.defaultProps = {
  menuContainer: document.body,
  type: FieldType.Unknown,
};

export default ValueActions;
