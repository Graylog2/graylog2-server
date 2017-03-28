import React, { PropTypes } from 'react';
import { Row, Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { Input } from 'components/bootstrap';
import Routes from 'routing/Routes';
import UserNotification from 'util/UserNotification';
import FormUtils from 'util/FormsUtils';

import StoreProvider from 'injection/StoreProvider';
const ToolsStore = StoreProvider.getStore('Tools');

const GrokExtractorConfiguration = React.createClass({
  propTypes: {
    configuration: PropTypes.object.isRequired,
    exampleMessage: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    onExtractorPreviewLoad: PropTypes.func.isRequired,
  },
  getInitialState() {
    return {
      trying: false,
    };
  },
  _onChange(key) {
    return (event) => {
      this.props.onExtractorPreviewLoad(undefined);
      const newConfig = this.props.configuration;
      newConfig[key] = FormUtils.getValueFromInput(event.target);
      this.props.onChange(newConfig);
    };
  },
  _onTryClick() {
    this.setState({ trying: true });

    const promise = ToolsStore.testGrok(this.props.configuration.grok_pattern, this.props.configuration.named_captures_only, this.props.exampleMessage);
    promise.then((result) => {
      if (!result.matched) {
        UserNotification.warning('We were not able to run the grok extraction. Please check your parameters.');
        return;
      }

      const matches = [];
      result.matches.map((match) => {
        matches.push(<dt key={`${match.name}-name`}>{match.name}</dt>);
        matches.push(<dd key={`${match.name}-value`}><samp>{match.match}</samp></dd>);
      });

      const preview = (matches.length === 0 ? '' : <dl>{matches}</dl>);
      this.props.onExtractorPreviewLoad(preview);
    });

    promise.finally(() => this.setState({ trying: false }));
  },
  _isTryButtonDisabled() {
    return this.state.trying || !this.props.configuration.grok_pattern || !this.props.exampleMessage;
  },
  render() {
    const helpMessage = (
      <span>
          Matches the field against the current Grok pattern list, use <b>{'%{PATTERN-NAME}'}</b> to refer to a{' '}
        <LinkContainer to={Routes.SYSTEM.GROKPATTERNS}><a>stored pattern</a></LinkContainer>.
        </span>
    );

    return (
      <div>
        <Input type="checkbox"
               id="named_captures_only"
               label="Named captures only"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked={this.props.configuration.named_captures_only}
               onChange={this._onChange('named_captures_only')}
               help="Only put the explicitly named captures into the message." />

        <Input id="grok_pattern"
               label="Grok pattern"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               help={helpMessage}>
          <Row className="row-sm">
            <Col md={11}>
              <input type="text" id="grok_pattern" className="form-control"
                     defaultValue={this.props.configuration.grok_pattern}
                     onChange={this._onChange('grok_pattern')}
                     required />
            </Col>
            <Col md={1} className="text-right">
              <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
                {this.state.trying ? <i className="fa fa-spin fa-spinner" /> : 'Try'}
              </Button>
            </Col>
          </Row>
        </Input>
      </div>
    );
  },
});

export default GrokExtractorConfiguration;
