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
import { isEmpty } from 'lodash';
import { useFormikContext } from 'formik';
import { useCallback } from 'react';

import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import IconButton from 'components/common/IconButton';

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

const Header = styled.div(({ theme, $hasFormValues }: { theme: DefaultTheme, $hasFormValues: boolean }) => css`
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
    border-bottom: 1px solid ${$hasFormValues ? theme.utils.contrastingColor(theme.colors.global.contentBackground, 'AA') : theme.colors.gray['70']};
    position: absolute;
  }
`);

const ElementTitle = styled.div(({ theme, $hasFormValues }: { theme: DefaultTheme, $hasFormValues: boolean }) => css`
  background-color: ${theme.colors.global.contentBackground};
  color: ${$hasFormValues ? theme.colors.global.textDefault : theme.colors.gray['70']};
  z-index: 1;
  padding-right: 8px;
`);

const ElementActions = styled.div(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  z-index: 1;
  padding-left: 5px;
`);

const StyledIconButton = styled(IconButton)(({ theme }) => `
  color: ${theme.colors.global.textDefault};
`);

const SectionHeader = ({ elementKey, onCreate, title, allowCreate, elementTitle }: {
  elementTitle: string,
  onCreate: (
    elementKey: string,
    values: WidgetConfigFormValues,
    setValues: (formValues: WidgetConfigFormValues) => void,
  ) => void,
  title: string,
  allowCreate: boolean,
  elementKey: string,
}) => {
  const { values, setValues } = useFormikContext<WidgetConfigFormValues>();
  const hasFormValues = !isEmpty(values[elementKey]);
  const createNewElement = useCallback(() => onCreate(elementKey, values, setValues), [elementKey, onCreate, setValues, values]);

  return (
    <Header $hasFormValues={hasFormValues}>
      <ElementTitle $hasFormValues={hasFormValues}>
        {title}
      </ElementTitle>
      <ElementActions>
        {allowCreate && (
          <StyledIconButton title={`Add a ${elementTitle}`} name="plus" onClick={createNewElement} />
        )}
      </ElementActions>
    </Header>
  );
};

type Props = {
  allowCreate: boolean,
  children: React.ReactNode,
  elementTitle: string,
  onCreate: (
    elementKey: string,
    values: WidgetConfigFormValues,
    setValues: (formValues: WidgetConfigFormValues) => void,
  ) => void,
  sectionTitle?: string,
  elementKey: string,
}

const ElementConfigurationSection = ({
  allowCreate,
  children,
  elementTitle,
  onCreate,
  sectionTitle,
  elementKey,
}: Props) => {
  const title = sectionTitle ?? elementTitle;

  return (
    <Wrapper data-testid={`${title}-section`}>
      <SectionHeader elementKey={elementKey}
                     onCreate={onCreate}
                     elementTitle={elementTitle}
                     title={title}
                     allowCreate={allowCreate} />
      <div>
        {children}
      </div>
    </Wrapper>
  );
};

ElementConfigurationSection.defaultProps = {
  sectionTitle: undefined,
};

export default ElementConfigurationSection;
