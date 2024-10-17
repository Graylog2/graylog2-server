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
import { forwardRef, useMemo, useCallback } from 'react';
import { useTheme } from 'styled-components';
import type { IMarker } from 'react-ace';

import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';

import StyledAceEditor from './StyledAceEditor';
import type { Editor } from './ace-types';

const ACE_THEME = 'ace-queryinput';

export type BaseProps = {
  className?: string
  error?: QueryValidationState,
  height?: number,
  maxLines?: number,
  onLoad?: (editor: Editor) => void,
  placeholder?: string,
  value: string,
  warning?: QueryValidationState,
  wrapEnabled?: boolean,
  inputId?: string,
};

type EnabledInputProps = BaseProps & {
  disabled: false,
  enableAutocompletion?: boolean,
  onBlur?: (query: string) => void,
  onChange: (query: string) => Promise<string>,
  onExecute: (editor: Editor) => void,
};
type DisabledInputProps = BaseProps & { disabled: true };
type Props = EnabledInputProps | DisabledInputProps

const isEnabledInput = (props: Props): props is EnabledInputProps => !props.disabled;
const isDisabledInput = (props: Props): props is DisabledInputProps => props.disabled;

const getMarkers = (errors: QueryValidationState | undefined, warnings: QueryValidationState | undefined) => {
  const markerClassName = 'ace_marker';
  const createMarkers = (explanations: QueryValidationState['explanations'] = [], className: string = ''): IMarker[] => explanations.map(({
    beginLine,
    beginColumn,
    endLine,
    endColumn,
  }) => ({
    startRow: beginLine,
    startCol: beginColumn,
    endRow: endLine,
    endCol: endColumn,
    type: 'text',
    className,
  }));

  return [
    ...createMarkers(errors?.explanations, `${markerClassName} ace_validation_error`),
    ...createMarkers(warnings?.explanations, `${markerClassName} ace_validation_warning`),
  ];
};

// Basic query input component which is being implemented by the `QueryInput` component.
// This is just a very basic query input which can be implemented for example to display a read only query.
const BasicQueryInput = forwardRef<any, Props>((props, ref) => {
  const {
    className = '',
    disabled = false,
    error,
    height,
    maxLines = 4,
    placeholder = '',
    value = '',
    warning,
    wrapEnabled = true,
    onLoad,
    inputId,
  } = props;
  const theme = useTheme();
  const markers = useMemo(() => getMarkers(error, warning), [error, warning]);
  const _onLoad = useCallback((editor) => {
    if (editor) {
      editor.renderer.setScrollMargin(7, 6);
      editor.renderer.setPadding(12);

      if (inputId) {
        editor.textInput.getElement().setAttribute('id', inputId);
      }

      onLoad?.(editor);
    }
  }, [inputId, onLoad]);
  const editorProps = useMemo(() => ({ $blockScrolling: Infinity, selectionStyle: 'line' }), []);
  const setOptions = useMemo(() => ({ indentedSoftWrap: false }), []);

  const commonProps = {
    $height: height,
    $scTheme: theme,
    theme: ACE_THEME,
    className: `${className} ${ACE_THEME}`,
    disabled,
    editorProps,
    fontSize: theme.fonts.size.small,
    highlightActiveLine: false,
    markers,
    maxLines,
    minLines: 1,
    mode: 'lucene',
    name: 'QueryEditor',
    placeholder,
    readOnly: disabled,
    ref,
    setOptions,
    showGutter: false,
    showPrintMargin: false,
    value,
    wrapEnabled,
    onLoad: _onLoad,
  };

  if (isDisabledInput(props)) {
    return <StyledAceEditor {...commonProps} disabled />;
  }

  if (isEnabledInput(props)) {
    const {
      onBlur,
      onChange,
      enableAutocompletion = false,
    } = props;

    return (
      <StyledAceEditor {...commonProps}
                       enableBasicAutocompletion={enableAutocompletion}
                       enableLiveAutocompletion={enableAutocompletion}
                       onBlur={onBlur}
                       onChange={onChange} />
    );
  }

  return null;
});

export default React.memo(BasicQueryInput);
