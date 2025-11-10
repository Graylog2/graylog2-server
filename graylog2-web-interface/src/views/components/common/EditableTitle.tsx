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
import { useCallback } from 'react';
import styled, { css } from 'styled-components';

import useDisclosure from 'util/hooks/useDisclosure';

import styles from './EditableTitle.css';

export const Title = styled.span(
  ({ theme }) => css`
    border: 1px solid ${theme.colors.global.contentBackground};
    font-size: ${theme.fonts.size.large};
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
  `,
);

const StyledInput = styled.input(
  ({ theme }) => css`
    border: 1px solid ${theme.colors.input.border};
    background-color: ${theme.colors.input.background};
    color: ${theme.colors.input.color};
    border-radius: 4px;
    padding: 2px 3px;
    font-size: ${theme.fonts.size.large};

    &:focus {
      border-color: ${theme.colors.input.borderFocus};
      outline: none;
    }
  `,
);

type Props = {
  disabled?: boolean;
  onChange: (newTitle: string) => void;
  value: string;
};

const EditableTitle = ({ disabled = false, value: propsValue, onChange }: Props) => {
  const [value, setValue] = React.useState(propsValue);
  const [editing, { toggle }] = useDisclosure(false);

  const _toggleEditing = useCallback(() => {
    if (!disabled) {
      toggle();
    }
  }, [disabled, toggle]);

  const _onChange = useCallback((evt: React.ChangeEvent<HTMLInputElement>) => {
    evt.preventDefault();
    setValue(evt.target.value);
  }, []);

  const _submitValue = useCallback(() => {
    if (value !== '') {
      onChange?.(value);
    } else {
      setValue(propsValue);
    }
  }, [onChange, propsValue, value]);

  const _onBlur = useCallback(() => {
    _toggleEditing();
    _submitValue();
  }, [_submitValue, _toggleEditing]);

  const _onSubmit = useCallback(
    (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      e.stopPropagation();
      _toggleEditing();
      _submitValue();
    },
    [_submitValue, _toggleEditing],
  );

  return editing ? (
    <span>
      <form onSubmit={_onSubmit} className={styles.inlineForm}>
        <StyledInput autoFocus type="text" value={value} onBlur={_onBlur} title="Edit title" onChange={_onChange} />
      </form>
    </span>
  ) : (
    <Title onDoubleClick={_toggleEditing} title={`${value} - Double click the title to edit it.`}>
      {value}
    </Title>
  );
};

export default EditableTitle;
