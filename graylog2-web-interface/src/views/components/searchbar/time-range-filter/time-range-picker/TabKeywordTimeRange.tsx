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

import { Col, FormControl, FormGroup, Panel, Row } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import ToolsStore from 'stores/tools/ToolsStore';
import type { KeywordTimeRange } from 'views/logic/queries/Query';
import useUserDateTime from 'hooks/useUserDateTime';
import { InputDescription } from 'components/common';
import debounceWithPromise from 'views/logic/debounceWithPromise';

import { EMPTY_RANGE } from '../TimeRangeDisplay';

type KeywordPreview = { from: string, to: string, timezone: string }

const Headline = styled.h3`
  margin-bottom: 5px;
`;

const KeywordInput = styled(FormControl)(({ theme }) => css`
  min-height: 34px;
  font-size: ${theme.fonts.size.large};
`);

const EffectiveTimeRangeTable = styled.table`
  margin-bottom: 5px;

  td:first-child {
    padding-right: 10px;
  }
`;

const _parseKeywordPreview = (data: Pick<KeywordTimeRange, 'from' | 'to' | 'timezone'>, formatTime: (dateTime: string, format: string) => string) => {
  const { timezone } = data;

  return {
    from: formatTime(data.from, 'complete'),
    to: formatTime(data.to, 'complete'),
    timezone,
  };
};

type Props = {
  defaultValue: string,
  disabled: boolean,
  setValidatingKeyword: (isValidating: boolean) => void
};

const debouncedTestNaturalDate = debounceWithPromise((
  keyword: string,
  userTZ: string,
  mounted: React.RefObject<boolean>,
  setSuccessfulPreview: (response: KeywordPreview) => void,
  setFailedPreview: () => void,
) => ToolsStore.testNaturalDate(keyword, userTZ).then((response) => {
  if (mounted.current) {
    setSuccessfulPreview(response);
  }
}).catch(() => {
  setFailedPreview();

  return 'Unable to parse keyword.';
}), 350);

const TabKeywordTimeRange = ({ defaultValue, disabled, setValidatingKeyword }: Props) => {
  const { formatTime, userTimezone } = useUserDateTime();
  const [nextRangeProps, , nextRangeHelpers] = useField('timeRangeTabs.keyword');
  const mounted = useRef(true);
  const keywordRef = useRef<string>();
  const [keywordPreview, setKeywordPreview] = useState<KeywordPreview>({ from: '', to: '', timezone: userTimezone });

  const _setSuccessfulPreview = useCallback((response: KeywordPreview) => {
    setValidatingKeyword(false);

    setKeywordPreview(_parseKeywordPreview(response, formatTime));
  },
  [setValidatingKeyword, formatTime]);

  const _setFailedPreview = useCallback(() => {
    setKeywordPreview({ from: EMPTY_RANGE, to: EMPTY_RANGE, timezone: userTimezone });
  }, [userTimezone]);

  const _validateKeyword = useCallback((keyword: string) => {
    if (keyword === undefined) {
      return undefined;
    }

    if (keywordRef.current !== keyword) {
      keywordRef.current = keyword;

      setValidatingKeyword(true);

      return trim(keyword) === ''
        ? Promise.resolve('Keyword must not be empty!')
        : debouncedTestNaturalDate(keyword, userTimezone, mounted, _setSuccessfulPreview, _setFailedPreview);
    }

    return undefined;
  }, [_setFailedPreview, _setSuccessfulPreview, setValidatingKeyword, userTimezone]);

  useEffect(() => () => {
    mounted.current = false;
  }, []);

  useEffect(() => {
    _validateKeyword(keywordRef.current);
  }, [_validateKeyword]);

  useEffect(() => {
    if (nextRangeProps?.value) {
      const { type, keyword, ...restPreview } = nextRangeProps.value;

      if (!isEqual(restPreview, keywordPreview)) {
        nextRangeHelpers.setValue({
          type,
          keyword,
          ...restPreview,
          ...keywordPreview,
        }, false);
      }
    }
  }, [nextRangeProps.value, keywordPreview, nextRangeHelpers]);

  useEffect(() => () => {
    setValidatingKeyword(false);
  }, [setValidatingKeyword]);

  return (
    <Row className="no-bm">
      <Col sm={5}>
        <Headline>Time range:</Headline>
        <Field name="timeRangeTabs.keyword.keyword" validate={_validateKeyword}>
          {({ field: { name, value, onChange }, meta: { error } }) => (
            <FormGroup controlId="form-inline-keyword"
                       style={{ marginRight: 5, width: '100%', marginBottom: 0 }}
                       validationState={error ? 'error' : null}>
              <KeywordInput type="text"
                            className="input-sm mousetrap"
                            name={name}
                            disabled={disabled}
                            placeholder="Last week"
                            title="Keyword input"
                            aria-label="Keyword input"
                            onChange={onChange}
                            required
                            value={value || defaultValue} />

              <InputDescription error={error} help="Specify the time frame for the search in natural language." />
            </FormGroup>
          )}
        </Field>

        <b>Preview</b>
        <EffectiveTimeRangeTable>
          <tbody>
            <tr>
              <td>From</td>
              <td>{keywordPreview.from}</td>
            </tr>
            <tr>
              <td>To</td>
              <td>{keywordPreview.to}</td>
            </tr>
          </tbody>
        </EffectiveTimeRangeTable>
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

TabKeywordTimeRange.propTypes = {
  defaultValue: PropTypes.string,
  disabled: PropTypes.bool,
  setValidatingKeyword: PropTypes.func,
};

TabKeywordTimeRange.defaultProps = {
  defaultValue: '',
  disabled: false,
  setValidatingKeyword: () => {},
};

export default TabKeywordTimeRange;
