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
import PropTypes from 'prop-types';
import { useTheme } from 'styled-components';

import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';

import StyledAceEditor from './StyledAceEditor';
import type { Editor } from './ace-types';

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
  const createMarkers = (explanations = [], className = '') => explanations.map(({
    beginLine,
    beginColumn,
    endLine,
    endColumn,
  }) => ({
    startRow: beginLine,
    startCol: beginColumn,
    endRow: endLine,
    endCol: endColumn,
    type: 'background',
    className,
  }));

  return [
    ...createMarkers(errors?.explanations, `${markerClassName} ace_validation_error`),
    ...createMarkers(warnings?.explanations, `${markerClassName} ace_validation_warning`),
  ];
};

// Basic query input component which is being implemented by the `QueryInput` component.
// This is just a very basic query input which can be implemented for example to display a read only query.
const BasicQueryInput = forwardRef<StyledAceEditor, Props>((props, ref) => {
  const {
    className,
    disabled,
    error,
    height,
    maxLines,
    placeholder,
    value,
    warning,
    wrapEnabled,
    onLoad,
    inputId,
  } = props;
  const theme = useTheme();
  const markers = useMemo(() => getMarkers(error, warning), [error, warning]);
  const _onLoad = useCallback((editor) => {
    if (editor) {
      editor.renderer.setScrollMargin(6, 5);
      editor.renderer.setPadding(12);
      editor.textInput.getElement().setAttribute('id', inputId);
      onLoad?.(editor);
    }
  }, [onLoad]);
  const editorProps = useMemo(() => ({ $blockScrolling: Infinity, selectionStyle: 'line' }), []);
  const setOptions = useMemo(() => ({ indentedSoftWrap: false }), []);

  const commonProps = {
    $height: height,
    aceTheme: 'ace-queryinput', // NOTE: is usually just `theme` but we need that prop for styled-components
    className,
    disabled,
    editorProps,
    fontSize: theme.fonts.size.large,
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
      onExecute,
      enableAutocompletion,
    } = props;

    return (
      <StyledAceEditor {...commonProps}
                       enableBasicAutocompletion={enableAutocompletion}
                       enableLiveAutocompletion={enableAutocompletion}
                       onBlur={onBlur}
                       name="just-a-test"
                       onChange={onChange}
                       onExecute={onExecute} />
    );
  }

  return null;
});

BasicQueryInput.propTypes = {
  className: PropTypes.string,
  // @ts-ignore
  disabled: PropTypes.bool,
  enableAutocompletion: PropTypes.bool,
  error: PropTypes.any,
  height: PropTypes.number,
  inputId: PropTypes.string,
  maxLines: PropTypes.number,
  onBlur: PropTypes.func,
  onChange: PropTypes.func,
  onExecute: PropTypes.func,
  onLoad: PropTypes.func,
  placeholder: PropTypes.string,
  value: PropTypes.string,
  warning: PropTypes.any,
  wrapEnabled: PropTypes.bool,
};

BasicQueryInput.defaultProps = {
  className: '',
  disabled: false,
  enableAutocompletion: false,
  error: undefined,
  height: undefined,
  inputId: 'query-input-id',
  maxLines: 4,
  onBlur: undefined,
  onChange: undefined,
  onExecute: undefined,
  onLoad: undefined,
  placeholder: '',
  value: '',
  warning: undefined,
  wrapEnabled: true,
};

export default React.memo(BasicQueryInput);
