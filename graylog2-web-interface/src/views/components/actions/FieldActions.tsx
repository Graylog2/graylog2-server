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
import styled, { css } from 'styled-components';

import type FieldType from 'views/logic/fieldtypes/FieldType';
import { ActionContext } from 'views/logic/ActionContext';
import type { QueryId } from 'views/logic/queries/Query';
import Action from 'views/components/actions/Action';

type Props = {
  children: React.ReactNode,
  disabled: boolean,
  element: React.ReactNode,
  menuContainer: HTMLElement | undefined | null,
  name: string,
  queryId: QueryId,
  type: FieldType,
};

type FieldElementProps = {
  $active: boolean,
  $disabled: boolean,
};

const FieldElement = styled.span.attrs({
  className: 'field-element' /* stylelint-disable-line property-no-unknown*/
})<FieldElementProps>(({ $active, $disabled, theme }) => css`
  color: ${$active ? theme.colors.variant.info : 'currentColor'};
  opacity: ${$disabled ? '0.3' : '1'};
`);

const FieldActions = ({ children, disabled, element, menuContainer, name, type, queryId }: Props) => {
  const actionContext = useContext(ActionContext);
  const wrappedElement = ({ active }: { active: boolean }) => (
    <FieldElement $active={active}
                  $disabled={disabled}>{element}
    </FieldElement>
  );
  const handlerArgs = { queryId, field: name, type, contexts: actionContext };

  return (
    <Action element={wrappedElement} handlerArgs={handlerArgs} menuContainer={menuContainer} type="field">
      {children}
    </Action>
  );
};

export default FieldActions;
