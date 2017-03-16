import React from 'react';
import ReactDOM from 'react-dom';
import { Button, Row, Col } from 'react-bootstrap';

import StoreProvider from 'injection/StoreProvider';
const UsageStatsOptOutStore = StoreProvider.getStore('UsageStatsOptOut');

const UsageStatsOptOut = React.createClass({
  getInitialState() {
    return {
      optOutStateLoaded: false,
      optOutState: null,
      pluginEnabled: false,
      buttonWidth: 80,
    };
  },
  componentDidMount() {
    UsageStatsOptOutStore.pluginEnabled().done((isEnabled) => {
      this.setState({ pluginEnabled: isEnabled }, this._updateOkButtonWidth);
    });

    UsageStatsOptOutStore.getOptOutState().done((optOutState) => {
      this.setState({ optOutStateLoaded: true, optOutState: optOutState }, this._updateOkButtonWidth);
    });
  },
  _updateOkButtonWidth() {
    if (this.refs.dontSendButton) {
      this.setState({ buttonWidth: ReactDOM.findDOMNode(this.refs.dontSendButton).clientWidth });
    }
  },
  _handleClickEnable() {
    UsageStatsOptOutStore.setOptIn(true);
    this.setState({ optOutStateLoaded: true, optOutState: { opt_out: false } });
  },
  _handleClickDisable() {
    UsageStatsOptOutStore.setOptOut(true);
    this.setState({ optOutStateLoaded: true, optOutState: { opt_out: true } });
  },
  render() {
    let content = null;

    if (this.state.optOutStateLoaded) {
      // We only show the opt-out form if there is no opt-out state!
      if (this.state.pluginEnabled === true && this.state.optOutState === null) {
        content = (
          <Row className="content">
            <Col md={12}>
              <Row className="row-sm">
                <Col md={10}>
                  <div style={{ marginTop: 8 }}>
                    <i className="fa fa-info-circle" />
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
                            onClick={this._handleClickEnable}
                            style={{ width: this.state.buttonWidth }}>
                      Ok
                    </Button>
                    &nbsp;
                    <Button ref="dontSendButton" bsSize="small" onClick={this._handleClickDisable}>
                      Don't send
                    </Button>
                  </div>
                </Col>
              </Row>
            </Col>
          </Row>
        );
      }
    }

    return content;
  },
});

export default UsageStatsOptOut;
