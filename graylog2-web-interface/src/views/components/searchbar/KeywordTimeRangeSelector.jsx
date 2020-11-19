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
import { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';
import { trim } from 'lodash';
import { connect, Field, useFormikContext } from 'formik';

import { Alert, Col, FormControl, FormGroup, InputGroup, Row, Tooltip } from 'components/graylog';
import DateTime from 'logic/datetimes/DateTime';
import StoreProvider from 'injection/StoreProvider';
import type { ThemeInterface } from 'theme';

const ToolsStore = StoreProvider.getStore('Tools');

const KeywordPreview: StyledComponent<{}, void, *> = styled(Alert)`
  display: flex;
  align-items: center;
  min-height: 34px;
  padding-top: 5px;
  padding-bottom: 5px;
  margin-top: 0 !important;  /* Would be overwritten by graylog.less */
`;

const KeywordInput: StyledComponent<{}, ThemeInterface, *> = styled(FormControl)(({ theme }) => css`
  min-height: 34px;
  font-size: ${theme.fonts.size.large};
`);

const StyledTooltip = styled(Tooltip)`
  white-space: nowrap;
`;

const _parseKeywordPreview = (data) => {
  const from = DateTime.fromUTCDateTime(data.from).toString();
  const to = DateTime.fromUTCDateTime(data.to).toString();

  return Immutable.Map({ from, to });
};

type Props = {
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

const KeywordTimeRangeSelector = ({ disabled }: Props) => {
  const [keywordPreview, setKeywordPreview] = useState(Immutable.Map());
  const _setSuccessfullPreview = useCallback(
    (response: { from: string, to: string }) => setKeywordPreview(_parseKeywordPreview(response)),
    [setKeywordPreview],
  );
  const _setFailedPreview = useCallback(() => {
    setKeywordPreview(Immutable.Map());

    return 'Unable to parse keyword.';
  }, [setKeywordPreview]);

  const formik = useFormikContext();

  const _validate = useCallback(
    (newKeyword) => _validateKeyword(newKeyword, _setSuccessfullPreview, _setFailedPreview),
    [_setSuccessfullPreview, _setFailedPreview],
  );

  useEffect(() => {
    const { values: { timerange: { keyword } } } = formik;

    ToolsStore.testNaturalDate(keyword)
      .then(_setSuccessfullPreview, _setFailedPreview);

    return () => formik.unregisterField('timerange.keyword');
  }, []);

  const { from, to } = keywordPreview.toObject();
  const keywordPreviewElement = !keywordPreview.isEmpty() && (
    <KeywordPreview bsStyle="info">
      <strong style={{ marginRight: 8 }}>Preview:</strong>
      {from} to {to}
    </KeywordPreview>
  );

  return (
    <Row className="no-bm" style={{ marginLeft: 50 }}>
      <Col xs={3} style={{ padding: 0 }}>
        <Field name="timerange.keyword" validate={_validate}>
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
                              value={value} />
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
  disabled: PropTypes.bool,
};

KeywordTimeRangeSelector.defaultProps = {
  disabled: false,
};

export default connect(KeywordTimeRangeSelector);
