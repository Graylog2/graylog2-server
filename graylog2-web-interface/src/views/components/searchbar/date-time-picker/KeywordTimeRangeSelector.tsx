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
import { useCallback, useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import trim from 'lodash/trim';
import isEqual from 'lodash/isEqual';
import { Field, useField } from 'formik';

import { Col, FormControl, FormGroup, Panel, Row } from 'components/graylog';
import DateTime from 'logic/datetimes/DateTime';
import StoreProvider from 'injection/StoreProvider';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import { EMPTY_RANGE } from '../TimeRangeDisplay';

const ToolsStore = StoreProvider.getStore('Tools');

const KeywordInput = styled(FormControl)(({ theme }) => css`
  min-height: 34px;
  font-size: ${theme.fonts.size.large};
`);

const ErrorMessage = styled.span(({ theme }) => css`
  color: ${theme.colors.variant.dark.danger};
  font-size: ${theme.fonts.size.tiny};
  font-style: italic;
  padding: 3px 3px 9px;
  display: block;
`);

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
  _setSuccessfullPreview: (preview: { from: string, to: string }) => void,
  _setFailedPreview: () => string,
): Promise<string> | undefined | null => {
  if (keyword === undefined) {
    return undefined;
  }

  return trim(keyword) === ''
    ? Promise.resolve('Keyword must not be empty!')
    : ToolsStore.testNaturalDate(keyword)
      .then(_setSuccessfullPreview, _setFailedPreview);
};

const KeywordTimeRangeSelector = ({ defaultValue, disabled }: Props) => {
  const [nextRangeProps, , nextRangeHelpers] = useField('nextTimeRange');
  const keywordRef = useRef();
  const [keywordPreview, setKeywordPreview] = useState({ from: '', to: '' });

  const _setSuccessfullPreview = useCallback(
    (response: { from: string, to: string }) => setKeywordPreview(_parseKeywordPreview(response)),
    [],
  );

  const _setFailedPreview = useCallback(() => {
    setKeywordPreview({ from: EMPTY_RANGE, to: EMPTY_RANGE });

    return 'Unable to parse keyword.';
  }, [setKeywordPreview]);

  const _validate = useCallback(
    (newKeyword) => _validateKeyword(newKeyword, _setSuccessfullPreview, _setFailedPreview),
    [_setSuccessfullPreview, _setFailedPreview],
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
    <Row className="no-bm">
      <Col sm={5}>
        <Field name="nextTimeRange.keyword" validate={_validate}>
          {({ field: { name, value, onChange }, meta: { error } }) => (
            <FormGroup controlId="form-inline-keyword"
                       style={{ marginRight: 5, width: '100%', marginBottom: 0 }}
                       validationState={error ? 'error' : null}>

              <p><strong>Specify the time frame for the search in natural language.</strong></p>
              <KeywordInput type="text"
                            className="input-sm"
                            name={name}
                            disabled={disabled}
                            placeholder="Last week"
                            onChange={onChange}
                            required
                            value={value || defaultValue} />

              {error && (<ErrorMessage>{error}</ErrorMessage>)}
            </FormGroup>
          )}
        </Field>
      </Col>

      <Col sm={7}>
        <Panel>
          <Panel.Body>
            <p><code>last month</code> searches between one month ago and now</p>

            <p><code>4 hours ago</code> searches between four hours ago and now</p>

            <p><code>1st of april to 2 days ago</code> searches between 1st of April and 2 days ago</p>

            <p><code>yesterday midnight +0200 to today midnight +0200</code> searches between yesterday midnight and today midnight in timezone +0200 - will be 22:00 in UTC</p>

            <p>Please consult the <DocumentationLink page={DocsHelper.PAGES.TIME_FRAME_SELECTOR}
                                                     title="Keyword Time Range Documentation"
                                                     text="documentation" /> for more details.
            </p>
          </Panel.Body>
        </Panel>
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
