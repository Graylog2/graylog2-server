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
import { useCallback, useEffect, useRef, useState, useContext } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import trim from 'lodash/trim';
import isEqual from 'lodash/isEqual';
import { Field, useField } from 'formik';

import { Col, FormControl, FormGroup, Panel, Row } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import ToolsStore from 'stores/tools/ToolsStore';
import UserDateTimeContext from 'contexts/UserDateTimeContext';
import type { KeywordTimeRange } from 'views/logic/queries/Query';

import { EMPTY_RANGE } from '../TimeRangeDisplay';

const KeywordInput = styled(FormControl)(({ theme }) => css`
  min-height: 34px;
  font-size: ${theme.fonts.size.large};
`);

const ErrorMessage = styled.span(({ theme }) => css`
  color: ${theme.colors.variant.dark.danger};
  font-size: ${theme.fonts.size.small};
  font-style: italic;
  padding: 3px 3px 9px;
  display: block;
`);

const _parseKeywordPreview = (data: Pick<KeywordTimeRange, 'from' | 'to' | 'timezone'>, formatTime: (dateTime: string, tz: string) => string) => {
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
  setValidatingKeyword: (boolean) => void
};

const TabKeywordTimeRange = ({ defaultValue, disabled, setValidatingKeyword }: Props) => {
  const { formatTime, userTimezone } = useContext(UserDateTimeContext);
  const [nextRangeProps, , nextRangeHelpers] = useField('nextTimeRange');
  const mounted = useRef(true);
  const keywordRef = useRef<string>();
  const [keywordPreview, setKeywordPreview] = useState({ from: '', to: '', timezone: '' });

  const _setSuccessfullPreview = useCallback((response: { from: string, to: string, timezone: string }) => {
    setValidatingKeyword(false);

    return setKeywordPreview(_parseKeywordPreview(response, formatTime));
  },
  [setValidatingKeyword, formatTime]);

  const _setFailedPreview = useCallback(() => {
    setKeywordPreview({ from: EMPTY_RANGE, to: EMPTY_RANGE, timezone: userTimezone });

    return 'Unable to parse keyword.';
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
        : ToolsStore.testNaturalDate(keyword, userTimezone)
          .then((response) => {
            if (mounted.current) _setSuccessfullPreview(response);
          })
          .catch(_setFailedPreview);
    }

    return undefined;
  }, [_setFailedPreview, _setSuccessfullPreview, setValidatingKeyword, userTimezone]);

  useEffect(() => {
    return () => {
      mounted.current = false;
    };
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
        });
      }
    }
  }, [nextRangeProps.value, keywordPreview, nextRangeHelpers]);

  return (
    <Row className="no-bm">
      <Col sm={5}>
        <Field name="nextTimeRange.keyword" validate={_validateKeyword}>
          {({ field: { name, value, onChange }, meta: { error } }) => (
            <FormGroup controlId="form-inline-keyword"
                       style={{ marginRight: 5, width: '100%', marginBottom: 0 }}
                       validationState={error ? 'error' : null}>

              <p><strong>Specify the time frame for the search in natural language.</strong></p>
              <KeywordInput type="text"
                            className="input-sm mousetrap"
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
