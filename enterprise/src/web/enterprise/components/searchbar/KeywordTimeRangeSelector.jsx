import React from 'react';
import PropTypes from 'prop-types';
import { Alert, Col, Row } from 'react-bootstrap';
import Immutable from 'immutable';

import Input from 'components/bootstrap/Input';
import DateTime from 'logic/datetimes/DateTime';
import StoreProvider from 'injection/StoreProvider';

const ToolsStore = StoreProvider.getStore('Tools');

export default class KeywordTimeRangeSelector extends React.Component {
  static propTypes = {
    value: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  state = {
    keywordPreview: Immutable.Map(),
  };

  _onKeywordPreviewLoaded = (data) => {
    const from = DateTime.fromUTCDateTime(data.from).toString();
    const to = DateTime.fromUTCDateTime(data.to).toString();
    this.setState({ keywordPreview: Immutable.Map({ from: from, to: to }) });
  };

  _resetKeywordPreview = () => {
    this.setState({ keywordPreview: Immutable.Map() });
  };

  _keywordSearchChanged = (event) => {
    const value = event.target.value;
    this.props.onChange('keyword', value);

    if (value === '') {
      this._resetKeywordPreview();
    } else {
      ToolsStore.testNaturalDate(value)
        .then(data => this._onKeywordPreviewLoaded(data))
        .catch(() => this._resetKeywordPreview());
    }
  };

  render() {
    const { value } = this.props;
    const { keywordPreview } = this.state;
    return (
      <div className="timerange-selector keyword" style={{ width: 650 }}>
        <Row className="no-bm" style={{ marginLeft: 50 }}>
          <Col md={5} style={{ padding: 0 }}>
            <Input type="text"
                   name="keyword"
                   value={value.get('keyword')}
                   onChange={this._keywordSearchChanged}
                   placeholder="Last week"
                   className="input-sm"
                   required />
          </Col>
          <Col md={7} style={{ paddingRight: 0 }}>
            {keywordPreview.size > 0 &&
            <Alert bsStyle="info" style={{ height: 30, paddingTop: 5, paddingBottom: 5, marginTop: 0 }}>
              <strong style={{ marginRight: 8 }}>Preview:</strong>
              {keywordPreview.get('from')} to {keywordPreview.get('to')}
            </Alert>
            }
          </Col>
        </Row>
      </div>
    );
  }
}
