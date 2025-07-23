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
import styled from 'styled-components';
import DOMPurify from 'dompurify';

import { SourceCodeEditor } from 'components/common';

const EditorStyles = styled.div<{ $width: string }>`
  & .ace_editor {
    width: ${({ $width }) => $width};
  }

  & .ace_cursor {
    border-color: ${({ theme }) => theme.colors.text.primary};
  }
`;

type Props = {
  autoFocus?: boolean;
  value: string;
  onChange: (mdValue: string) => void;
  width?: string;
  id?: string;
  readOnly?: boolean;
  height?: number;
  onBlur?: (mdValue: string) => void;
};

function MDBaseEditor({
  autoFocus = false,
  value,
  onChange,
  id = 'md-editor',
  readOnly = false,
  height = 200,
  width = '100%',
  onBlur = undefined,
}: Props) {
  const handleOnBlur = React.useCallback(() => {
    // Remove dangerous markdown
    const sanitizedValue = DOMPurify.sanitize(
      // Remove dangerous HTML
      DOMPurify.sanitize(value, { USE_PROFILES: { html: false } }),
    );

    if (onBlur) onBlur(sanitizedValue);
    else onChange(sanitizedValue);
  }, [onBlur, onChange, value]);

  return (
    <EditorStyles $width={width}>
      <SourceCodeEditor
        focus={autoFocus}
        id={id}
        mode="markdown"
        theme="light"
        toolbar={false}
        resizable={false}
        readOnly={readOnly}
        height={height}
        value={value}
        onChange={onChange}
        onBlur={handleOnBlur}
        onPaste={handleOnBlur}
      />
    </EditorStyles>
  );
}

export default MDBaseEditor;
