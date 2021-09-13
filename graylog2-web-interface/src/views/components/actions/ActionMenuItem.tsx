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
import { useContext } from 'react';
import styled from 'styled-components';
import { ActionContexts } from 'views/types';

import Icon from 'components/common/Icon';
import { MenuItem } from 'components/graylog';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import {
  ActionDefinition,
  createHandlerFor,
  ActionHandlerArguments,
  isExternalLinkAction,
  ExternalLinkAction,
  HandlerAction,
} from 'views/components/actions/ActionHandler';

const StyledMenuItem = styled(MenuItem)`
  && > a {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
`;

const ExternalLinkIcon = styled(Icon)(({ theme }) => `
  margin-left: ${theme.spacings.xxs};
`);

type Props = {
  action: ActionDefinition,
  handlerArgs: ActionHandlerArguments,
  onMenuToggle: () => void,
  overflowingComponents: React.ReactNode,
  setOverflowingComponents: (components: React.ReactNode) => void,
  type: 'field' | 'value',
}

type ExternalLinkItemProps = Pick<Props, 'handlerArgs' | 'onMenuToggle' | 'type'> & {
  action: ExternalLinkAction<ActionContexts>,
  disabled: boolean,
  field: string,
}

const ExternalLinkItem = ({ action, disabled, field, handlerArgs, onMenuToggle, type }: ExternalLinkItemProps) => {
  const { unsetWidgetFocusing } = useContext(WidgetFocusContext);

  const linkProps = {
    href: action.linkTarget(handlerArgs),
    target: '_blank',
    rel: 'noopener noreferrer',
  };

  const onSelect = () => {
    const { resetFocus = false } = action;

    if (resetFocus) {
      unsetWidgetFocusing();
    }

    onMenuToggle();
  };

  return (
    <StyledMenuItem disabled={disabled}
                    eventKey={{ action: type, field }}
                    onSelect={onSelect}
                    {...linkProps}>
      {action.title}
      <ExternalLinkIcon name="external-link" />
    </StyledMenuItem>
  );
};

type ActionHandlerItemProps = Pick<Props, 'handlerArgs' | 'onMenuToggle' | 'overflowingComponents' | 'setOverflowingComponents' | 'type'> & {
  action: HandlerAction<ActionContexts>,
  disabled: boolean,
  field: string,
};

const ActionHandlerItem = ({ disabled, action, handlerArgs, setOverflowingComponents, overflowingComponents, type, onMenuToggle }: ActionHandlerItemProps) => {
  const { unsetWidgetFocusing } = useContext(WidgetFocusContext);

  const setActionComponents = (fn) => {
    setOverflowingComponents(fn(overflowingComponents));
  };

  const handler = createHandlerFor(action, setActionComponents);

  const onSelect = () => {
    const { resetFocus = false } = action;

    if (resetFocus) {
      unsetWidgetFocusing();
    }

    onMenuToggle();

    handler(handlerArgs);
  };

  const { field } = handlerArgs;

  return (
    <StyledMenuItem disabled={disabled}
                    eventKey={{ action: type, field }}
                    onSelect={onSelect}>
      {action.title}
    </StyledMenuItem>
  );
};

const ActionMenuItem = ({ action, handlerArgs, setOverflowingComponents, overflowingComponents, type, onMenuToggle }: Props) => {
  const { isEnabled = () => true } = action;
  const actionDisabled = !isEnabled(handlerArgs);
  const { field } = handlerArgs;

  if (isExternalLinkAction(action)) {
    return <ExternalLinkItem action={action} disabled={actionDisabled} field={field} handlerArgs={handlerArgs} onMenuToggle={onMenuToggle} type={type} />;
  }

  return (
    <ActionHandlerItem action={action}
                       disabled={actionDisabled}
                       field={field}
                       handlerArgs={handlerArgs}
                       onMenuToggle={onMenuToggle}
                       overflowingComponents={overflowingComponents}
                       setOverflowingComponents={setOverflowingComponents}
                       type={type} />
  );
};

export default ActionMenuItem;
