import React, {PropTypes} from 'react';
import {Row, Col, Input, Button} from 'react-bootstrap';
import {LinkContainer} from 'react-router-bootstrap';
import Routes from 'routing/Routes';

const GrokExtractorConfiguration = React.createClass({
  propTypes: {
    configuration: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },
  getInitialState() {
    return {
      trying: false,
    };
  },
  _onTryClick() {
    this.setState({trying: true});
  },
  _isTryButtonDisabled() {
    return this.state.trying || (this.refs.grokPattern && this.refs.grokPattern.value === '');
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
        <Input id="grok_pattern"
               label="Grok pattern"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               help={helpMessage}>
          <Row className="row-sm">
            <Col md={11}>
              <input type="text" ref="grokPattern" id="grok_pattern" className="form-control"
                     defaultValue={this.props.configuration.grok_pattern}
                     onChange={this.props.onChange('grok_pattern')}
                     required/>
            </Col>
            <Col md={1} className="text-right">
              <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
                {this.state.trying ? 'Trying...' : 'Try'}
              </Button>
            </Col>
          </Row>
        </Input>
      </div>
    );
  },
});

export default GrokExtractorConfiguration;
