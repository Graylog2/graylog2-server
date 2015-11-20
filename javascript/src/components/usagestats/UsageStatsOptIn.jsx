'use strict';

import React from 'react';
import { Panel, Button, Row, Col } from 'react-bootstrap';
import { UsageStatsOptOutStore } from '../../stores/usagestats/UsageStatsOptOutStore';

const UsageStatsOptIn = React.createClass({
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
    UsageStatsOptOutStore.setOptIn(false);
    this.setState({optOutState: {opt_out: false}});
  },
  _handleClickDisable() {
    UsageStatsOptOutStore.setOptOut(false);
    this.setState({optOutState: {opt_out: true}});
  },
  render() {
    var content = null;

    if (this.state.optOutStateLoaded && this.state.pluginEnabled === true) {
      var form = null;

      if (this.state.optOutState !== null && this.state.optOutState.opt_out === true) {
        form = (
          <div>
            <p className="description">
              You have currently <strong>disabled</strong> sending usage statistics to Graylog. Please consider turning it back on to provide anonymous statistics that will help us make Graylog better for you.
            </p>
            <Button bsSize="small" bsStyle="success" onClick={this._handleClickEnable}>Enable</Button>
          </div>
        );
      } else {
        form = (
          <div>
            <p className="description">
              You have currently <strong>enabled</strong> sending usage statistics to Graylog. Thank you! User statistics help us make Graylog better. If you've changed your mind, click "Disable".
            </p>
            <Button bsSize="small" bsStyle="info" onClick={this._handleClickDisable}>Disable</Button>
          </div>
        );
      }

      content = (
        <Row className="content">
          <Col md={12}>
            <h2><i className="fa fa-bar-chart"></i> Anonymous Usage Statistics</h2>
            {form}
          </Col>
        </Row>
      );
    }

    return content;
  }
});

module.exports = UsageStatsOptIn;
