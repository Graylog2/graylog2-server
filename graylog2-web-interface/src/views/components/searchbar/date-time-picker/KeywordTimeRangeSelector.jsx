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
// @flow strict
import * as React from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';
import trim from 'lodash/trim';
import isEqual from 'lodash/isEqual';
import { Field, useField } from 'formik';

import { Alert, Col, FormControl, FormGroup, InputGroup, Row, Tooltip } from 'components/graylog';
import DateTime from 'logic/datetimes/DateTime';
import StoreProvider from 'injection/StoreProvider';
import type { ThemeInterface } from 'theme';

const ToolsStore = StoreProvider.getStore('Tools');

const KeywordPreview: StyledComponent<{}, void, typeof Alert> = styled(Alert)`
  display: flex;
  align-items: center;
  min-height: 34px;
  padding-top: 5px;
  padding-bottom: 5px;
  margin-top: 0;
`;

const KeywordInput: StyledComponent<{}, ThemeInterface, typeof FormControl> = styled(FormControl)(({ theme }) => css`
  min-height: 34px;
  font-size: ${theme.fonts.size.large};
`);

const StyledTooltip = styled(Tooltip)`
  white-space: nowrap;
`;

const _parseKeywordPreview = (data) => {
  const from = DateTime.fromUTCDateTime(data.from).toString();
  const to = DateTime.fromUTCDateTime(data.to).toString();

  return { from, to };
};

type Props = {
  defaultValue: string,
  disabled: boolean,
};

const _validateKeyword = (
  keyword: string,
  _setSuccessfullPreview: ({ from: string, to: string }) => void,
  _setFailedPreview: () => string,
): ?Promise<string> => {
  if (keyword === undefined) {
    return undefined;
  }

  return trim(keyword) === ''
    ? Promise.resolve('Keyword must not be empty!')
    : ToolsStore.testNaturalDate(keyword)
      .then(_setSuccessfullPreview, _setFailedPreview);
};

const KeywordTimeRangeSelector = ({ defaultValue, disabled }: Props) => {
  const [nextRangeProps, , nextRangeHelpers] = useField('tempTimeRange');
  const keywordRef = useRef();
  const [keywordPreview, setKeywordPreview] = useState({ from: '', to: '' });

  const _setSuccessfullPreview = useCallback(
    (response: { from: string, to: string }) => setKeywordPreview(_parseKeywordPreview(response)),
    [],
  );

  const _setFailedPreview = useCallback(() => {
    setKeywordPreview({ from: '', to: '' });

    return 'Unable to parse keyword.';
  }, [setKeywordPreview]);

  const _validate = useCallback(
    (newKeyword) => _validateKeyword(newKeyword, _setSuccessfullPreview, _setFailedPreview),
    [_setSuccessfullPreview, _setFailedPreview],
  );

  const keywordPreviewElement = (keywordPreview.from && keywordPreview.to) && (
    <KeywordPreview bsStyle="info">
      <strong style={{ marginRight: 8 }}>Preview:</strong>
      {keywordPreview.from} to {keywordPreview.to}
    </KeywordPreview>
  );

  useEffect(() => {
    if (keywordRef.current !== nextRangeProps?.value?.keyword) {
      keywordRef.current = nextRangeProps.value.keyword;

      ToolsStore.testNaturalDate(keywordRef.current)
        .then(_setSuccessfullPreview, _setFailedPreview);
    }
  });

  useEffect(() => {
    if (nextRangeProps?.value) {
      const { type, keyword, ...restPreview } = nextRangeProps?.value;

      if (!isEqual(restPreview, keywordPreview)) {
        nextRangeHelpers.setValue({
          type,
          keyword,
          ...restPreview,
          ...keywordPreview,
        });
      }
    }
  }, [nextRangeProps.value, keywordPreview, nextRangeHelpers]);

  return (
    <Row className="no-bm" style={{ marginLeft: 50 }}>
      <Col xs={3} style={{ padding: 0 }}>
        <Field name="tempTimeRange.keyword" validate={_validate}>
          {({ field: { name, value, onChange }, meta: { error } }) => (
            <FormGroup controlId="form-inline-keyword"
                       style={{ marginRight: 5, width: '100%', marginBottom: 0 }}
                       validationState={error ? 'error' : null}>
              <InputGroup>
                {error && (
                  <StyledTooltip placement="top" className="in" id="tooltip-top" positionTop="-30px">
                    {error}
                  </StyledTooltip>
                )}
                <KeywordInput type="text"
                              className="input-sm"
                              name={name}
                              disabled={disabled}
                              placeholder="Last week"
                              onChange={onChange}
                              required
                              value={value || defaultValue} />
              </InputGroup>
            </FormGroup>
          )}
        </Field>
      </Col>
      <Col xs={8} style={{ paddingRight: 0 }}>
        {keywordPreviewElement}
      </Col>
    </Row>
  );
};

KeywordTimeRangeSelector.propTypes = {
  defaultValue: PropTypes.string,
  disabled: PropTypes.bool,
};

KeywordTimeRangeSelector.defaultProps = {
  defaultValue: '',
  disabled: false,
};

export default KeywordTimeRangeSelector;
