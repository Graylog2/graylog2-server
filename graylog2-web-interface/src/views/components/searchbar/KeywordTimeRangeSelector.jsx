// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Alert, Col, FormControl, FormGroup, InputGroup, Row } from 'components/graylog';
import * as Immutable from 'immutable';
import styled from 'styled-components';
import { trim } from 'lodash';

import DateTime from 'logic/datetimes/DateTime';
import StoreProvider from 'injection/StoreProvider';
import { connect, Field } from 'formik';

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
  formik: {
    values: {
      timerange: {
        keyword: string,
      },
    },
  },
};

type State = {
  keywordPreview: Immutable.Map<string, mixed>,
};

class KeywordTimeRangeSelector extends React.Component<Props, State> {
  static defaultProps = {
    disabled: false,
  };

  constructor(props: Props) {
    super(props);

    const { formik: { values: { timerange: { keyword } } } } = props;

    this.state = {
      keywordPreview: Immutable.Map(),
    };

    ToolsStore.testNaturalDate(keyword)
      .then(this._setSuccessfullPreview)
      .catch(this._setFailedPreview);
  }

  _setSuccessfullPreview = (response: { from: string, to: string }) => this.setState({
    keywordPreview: _parseKeywordPreview(response),
  });

  _setFailedPreview = () => {
    this.setState({ keywordPreview: Immutable.Map() });
    return 'Unable to parse keyword.';
  };

  _validateKeyword = (keyword: string) => {
    return trim(keyword) === ''
      ? Promise.resolve('Keyword must not be empty!')
      : ToolsStore.testNaturalDate(keyword)
        .then(this._setSuccessfullPreview, this._setFailedPreview);
  };

  render() {
    const { keywordPreview } = this.state;
    const { disabled } = this.props;
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
          <Field name="timerange.keyword" validate={this._validateKeyword}>
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
  }
}

KeywordTimeRangeSelector.propTypes = {
  disabled: PropTypes.bool,
};

export default connect(KeywordTimeRangeSelector);
