// @flow strict
import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { Alert, Col, FormControl, FormGroup, InputGroup, Row } from 'components/graylog';
import * as Immutable from 'immutable';
import styled from 'styled-components';
import { trim } from 'lodash';

import DateTime from 'logic/datetimes/DateTime';
import StoreProvider from 'injection/StoreProvider';
import { connect, Field, useFormikContext } from 'formik';

const ToolsStore = StoreProvider.getStore('Tools');

const KeywordPreview = styled(Alert)`
  display: flex;
  align-items: center;
  min-height: 34px;
  padding-top: 5px;
  padding-bottom: 5px;
  margin-top: 0 !important;  /* Would be overwritten by graylog.less */
`;

const KeywordInput = styled(FormControl)`
  min-height: 34px;
  font-size: 14px;
`;

const _parseKeywordPreview = (data) => {
  const from = DateTime.fromUTCDateTime(data.from).toString();
  const to = DateTime.fromUTCDateTime(data.to).toString();
  return Immutable.Map({ from, to });
};

type Props = {
  disabled: boolean,
};

const _validateKeyword = (keyword: string, _setSuccessfullPreview, _setFailedPreview) => {
  if (!keyword) {
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

  useEffect(() => {
    const { values: { timerange: { keyword } } } = formik;
    ToolsStore.testNaturalDate(keyword)
      .then(_setSuccessfullPreview)
      .catch(_setFailedPreview);

    return () => formik.unregisterField('timerange.keyword');
  }, []);

  const _validate = useCallback(
    (newKeyword) => _validateKeyword(newKeyword, _setSuccessfullPreview, _setFailedPreview),
    [_setSuccessfullPreview, _setFailedPreview],
  );

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
                       style={{ marginRight: 5, width: '100%' }}
                       validationState={error ? 'error' : null}>
              <InputGroup>
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
