import React from 'react';
import { Alert, Button, Row, Col } from 'react-bootstrap';

import StoreProvider from 'injection/StoreProvider';
const UsageStatsOptOutStore = StoreProvider.getStore('UsageStatsOptOut');

const UsageStatsOptIn = React.createClass({
  getInitialState() {
    return {
      optOutStateLoaded: false,
      optOutState: null,
      pluginEnabled: false,
    };
  },
  componentDidMount() {
    UsageStatsOptOutStore.pluginEnabled().done((isEnabled) => {
      this.setState({ pluginEnabled: isEnabled });
    });

    UsageStatsOptOutStore.getOptOutState().done((optOutState) => {
      this.setState({ optOutStateLoaded: true, optOutState: optOutState });
    });
  },
  _handleClickEnable() {
    UsageStatsOptOutStore.setOptIn(false);
    this.setState({ optOutState: { opt_out: false } });
  },
  _handleClickDisable() {
    UsageStatsOptOutStore.setOptOut(false);
    this.setState({ optOutState: { opt_out: true } });
  },
  render() {
    let content = null;

    if (this.state.optOutStateLoaded && this.state.pluginEnabled === true) {
      let form = null;

      if (this.state.optOutState !== null && this.state.optOutState.opt_out === true) {
        form = (
          <span>
            <i className="fa fa-info-circle" />
            &nbsp;
            You have currently <strong>disabled</strong> sending usage statistics to Graylog. Please consider turning
            it back on to provide anonymous statistics that will help us make Graylog better for you.
            <Button bsSize="xsmall" bsStyle="success" className="pull-right" onClick={this._handleClickEnable}>Enable</Button>
          </span>
        );
      } else {
        form = (
          <span>
            <i className="fa fa-info-circle" />
            &nbsp;
            You have currently <strong>enabled</strong> sending anonymous usage statistics to Graylog. Thank you! User
            statistics help us make Graylog better. If you've changed your mind, click "Disable".
            <Button bsSize="xsmall" bsStyle="info" className="pull-right" onClick={this._handleClickDisable}>Disable</Button>
          </span>
        );
      }

      content = (
        <Row className="content">
          <Col md={12}>
            <h2>Anonymous usage statistics</h2>
            <Alert bsStyle="info">
              {form}
            </Alert>
          </Col>
        </Row>
      );
    }

    return content;
  },
});

export default UsageStatsOptIn;
