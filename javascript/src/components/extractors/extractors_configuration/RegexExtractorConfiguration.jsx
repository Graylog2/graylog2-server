import React, {PropTypes} from 'react';
import {Row, Col, Input, Button} from 'react-bootstrap';

import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import UserNotification from 'util/UserNotification';
import ToolsStore from 'stores/tools/ToolsStore';

const RegexExtractorConfiguration = React.createClass({
  propTypes: {
    configuration: PropTypes.object.isRequired,
    exampleMessage: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    onExtractorPreviewLoad: PropTypes.func.isRequired,
  },
  getInitialState() {
    return {
      trying: false,
    };
  },
  _onChange(key) {
    const onConfigurationChange = this.props.onChange(key);

    return (event) => {
      this.props.onExtractorPreviewLoad(undefined);
      onConfigurationChange(event);
    };
  },
  _onTryClick() {
    this.setState({trying: true});

    const promise = ToolsStore.testRegex(this.refs.regexValue.value, this.props.exampleMessage);
    promise.then(result => {
      if (!result.matched) {
        UserNotification.warning('Regular expression did not match.');
        return;
      }

      if (!result.match) {
        UserNotification.warning('Regular expression does not contain any matcher group to extract.');
        return;
      }

      this.props.onExtractorPreviewLoad(<samp>{result.match.match}</samp>);
    });

    promise.finally(() => this.setState({trying: false}));
  },
  _isTryButtonDisabled() {
    return this.state.trying || (this.refs.regexValue && this.refs.regexValue.value === '');
  },
  render() {
    const helpMessage = (
      <span>
        The regular expression used for extraction. First matcher group is used.{' '}
        Learn more in the <DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text="documentation"/>.
      </span>
    );

    return (
      <div>
        <Input label="Regular expression"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               help={helpMessage}>
          <Row className="row-sm">
            <Col md={11}>
              <input type="text" ref="regexValue" id="regex_value" className="form-control"
                     defaultValue={this.props.configuration.regex_value}
                     placeholder="^.*string(.+)$"
                     onChange={this._onChange('regex_value')}
                     required/>
            </Col>
            <Col md={1} className="text-right">
              <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
                {this.state.trying ? <i className="fa fa-spin fa-spinner"/> : 'Try'}
              </Button>
            </Col>
          </Row>
        </Input>
      </div>
    );
  },
});

export default RegexExtractorConfiguration;
