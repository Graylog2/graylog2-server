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
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';
import { useCallback } from 'react';
import { useFormikContext } from 'formik';

import IconButton from 'components/common/IconButton';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import type { AggregationElement } from 'views/components/aggregationwizard/AggregationElementType';

const Wrapper = styled.div(({ theme }) => css`
  border-radius: 6px;
  margin-bottom: 6px;

  :last-child {
    margin-bottom: 0;
  }

  div[class^="col-"] {
    padding-right: 0;
    padding-left: 0;
  }

  input {
    font-size: ${theme.fonts.size.body};
  }

  .form-group {
    margin: 0 0 3px 0;
  }

  .control-label {
    padding-left: 0;
    padding-right: 5px;
    padding-top: 5px;
    font-weight: normal;
    text-align: left;
    hyphens: auto;
  }

  .help-block {
    margin: 0;
    hyphens: auto;
  }

  .checkbox {
    min-height: auto;
  }
`);

const Header = styled.div(({ theme, $isEmpty }: { theme: DefaultTheme, $isEmpty: boolean }) => css`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1px;
  min-height: 26px;
  font-weight: bold;
  background-color: ${theme.colors.global.contentBackground};
  position: sticky;
  top: 0;
  z-index: 1;
  
  ::before {
    content: ' ';
    top: 50%;
    width: 100%;
    border-bottom: 1px solid ${$isEmpty ? theme.colors.gray['70'] : theme.utils.contrastingColor(theme.colors.global.contentBackground, 'AA')};
    position: absolute;
  }
`);

const ElementTitle = styled.div(({ theme, $isEmpty }: { theme: DefaultTheme, $isEmpty: boolean }) => css`
  background-color: ${theme.colors.global.contentBackground};
  color: ${$isEmpty ? theme.colors.gray['70'] : theme.colors.global.textDefault};
  z-index: 1;
  padding-right: 8px;
`);

const ElementActions = styled.div(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  z-index: 1;
  padding-left: 5px;
`);

const StyledIconButton = styled(IconButton)(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
`);

type Props = {
  aggregationElement: AggregationElement<keyof WidgetConfigFormValues>,
  children: React.ReactNode,
  onCreate: (
    elementKey: string,
    values: WidgetConfigFormValues,
    setValues: (formValues: WidgetConfigFormValues) => void,
  ) => void,
}

const ElementConfigurationSection = ({
  aggregationElement,
  children,
  onCreate,
}: Props) => {
  const { values, setValues } = useFormikContext<WidgetConfigFormValues>();
  const { title, sectionTitle, allowCreate, key: elementKey, isEmpty: isElementEmpty } = aggregationElement;
  const elementFormValues = values[elementKey];
  const isAllowCreate = allowCreate(values);
  const isEmpty = isElementEmpty(elementFormValues);
  const headerTitle = sectionTitle ?? title;

  const _onCreate = useCallback(
    () => onCreate(elementKey, values, setValues),
    [onCreate, setValues, elementKey, values],
  );

  return (
    <Wrapper data-testid={`${headerTitle}-section`}>
      <Header $isEmpty={isEmpty}>
        <ElementTitle $isEmpty={isEmpty}>{headerTitle}</ElementTitle>
        <ElementActions>
          {isAllowCreate && (
            <StyledIconButton title={`Add a ${title}`} name="plus" onClick={_onCreate} />
          )}
        </ElementActions>
      </Header>
      <div>
        {children}
      </div>
    </Wrapper>
  );
};

export default React.memo(ElementConfigurationSection);
