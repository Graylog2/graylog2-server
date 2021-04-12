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
import styled, { css } from 'styled-components';

import IconButton from 'components/common/IconButton';

const Wrapper = styled.div(({ theme }) => css`
  margin-bottom: 6px;
  border-radius: 6px;

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
  }

  .help-block {
    margin: 0;
  }
`);

const Header = styled.div(({ theme }) => css`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1px;
  min-height: 26px;
  font-weight: bold;
  position: relative;

  :before {
    content: ' ';
    top: 50%;
    width: 100%;
    border-bottom: 1px solid ${theme.utils.contrastingColor(theme.colors.global.contentBackground, 'AA')};
    position: absolute;
  }
`);

const ElementTitle = styled.div(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
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

type Props = {
  allowAddEmptyElement: boolean,
  children: React.ReactNode,
  onAddEmptyElement: () => void,
  title: string,
  titleSingular?: string,
}

const ElementConfigurationContainer = ({
  children,
  allowAddEmptyElement,
  onAddEmptyElement,
  title,
  titleSingular,
}: Props) => {
  return (
    <Wrapper>
      <Header>
        <ElementTitle>
          {title}
        </ElementTitle>
        <ElementActions>
          {allowAddEmptyElement && (
            <StyledIconButton title={`Add a ${titleSingular ?? title}`} name="plus" onClick={onAddEmptyElement} />
          )}
        </ElementActions>
      </Header>
      <div>
        {children}
      </div>
    </Wrapper>
  );
};

ElementConfigurationContainer.defaultProps = {
  titleSingular: undefined,
};

export default ElementConfigurationContainer;
