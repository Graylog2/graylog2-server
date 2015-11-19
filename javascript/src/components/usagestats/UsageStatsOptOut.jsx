import React from 'react';
import { Panel, Button, Row, Col } from 'react-bootstrap';
import { UsageStatsOptOutStore } from '../../stores/usagestats/UsageStatsOptOutStore';

const UsageStatsOptOut = React.createClass({
  getInitialState() {
    return {
      optOutStateLoaded: false,
      optOutState: null,
      pluginEnabled: false
    };
  },
  componentDidMount() {
    UsageStatsOptOutStore.pluginEnabled().done((isEnabled) => {
      this.setState({pluginEnabled: isEnabled});
    });

    UsageStatsOptOutStore.getOptOutState().done((optOutState) => {
      this.setState({optOutStateLoaded: true, optOutState: optOutState});
    });
  },
  _handleClickEnable() {
    UsageStatsOptOutStore.setOptIn(true);
    this.setState({optOutStateLoaded: true, optOutState: {opt_out: false}});
  },
  _handleClickDisable() {
    UsageStatsOptOutStore.setOptOut(true);
    this.setState({optOutStateLoaded: true, optOutState: {opt_out: true}});
  },
  render() {
    var content = null;

    if (this.state.optOutStateLoaded) {
      // We only show the opt-out form if there is no opt-out state!
      if (this.state.pluginEnabled === true && this.state.optOutState === null) {
        content = (
          <Row className="content">
            <Col md={12}>
              <Row className="row-sm">
                <Col md={10}>
                  <div style={{marginTop: 8}}>
                    <i className="fa fa-info-circle"></i>
                    &nbsp;
                    Graylog collects completely anonymous usage data to help us improve the product
                    for you.
                    Continuing means you are cool with sending us anonymous data. If this makes you
                    unhappy, click <em>Don't send</em> to disable.
                  </div>
                </Col>
                <Col md={2}>
                  <div className="text-right">
                    <Button bsSize="small" bsStyle="success"
                            onClick={this._handleClickEnable}>Ok</Button>
                    &nbsp;
                    <Button bsSize="small" onClick={this._handleClickDisable}>Don't send</Button>
                  </div>
                </Col>
              </Row>
            </Col>
          </Row>
        );
      }
    }

    return content;
  }
});

module.exports = UsageStatsOptOut;
