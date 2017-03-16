import React, { PropTypes } from 'react';
import { Button, Grid, Row, Col } from 'react-bootstrap';
import Qs from 'qs';

import { Spinner } from 'components/common';

import ActionsProvider from 'injection/ActionsProvider';
const GettingStartedActions = ActionsProvider.getActions('GettingStarted');

const GettingStarted = React.createClass({
  propTypes() {
    return {
      clusterId: PropTypes.string.isRequired,
      masterOs: PropTypes.string.isRequired,
      masterVersion: PropTypes.string.isRequired,
      gettingStartedUrl: PropTypes.string.isRequired,
      noDismissButton: PropTypes.bool,
      onDismiss: PropTypes.func,
    };
  },
  getInitialState() {
    return {
      guideLoaded: false,
      guideUrl: '',
      showStaticContent: false,
      frameHeight: '500px',
    };
  },
  componentDidMount() {
    if (window.addEventListener) {
      window.addEventListener('message', this._onMessage);
    }
    this.timeoutId = window.setTimeout(this._displayFallbackContent, 3000);
  },
  componentWillUnmount() {
    if (window.removeEventListener) {
      window.removeEventListener('message', this._onMessage);
    }
    if (this.timeoutId !== null) {
      window.clearTimeout(Number(this.timeoutId));
      this.timeoutId = null;
    }
  },

  timeoutId: null,
  _onMessage(messageEvent) {
    // make sure we only process messages from the getting started url, otherwise this can interfere with other messages being posted
    if (this.props.gettingStartedUrl.indexOf(messageEvent.origin) === 0) {
      if (this.timeoutId !== null) {
        window.clearTimeout(Number(this.timeoutId));
        this.timeoutId = null;
      }
      this.setState({
        guideLoaded: messageEvent.data.guideLoaded,
        guideUrl: messageEvent.data.guideUrl,
        minHeight: messageEvent.data.height === 0 ? this.state.minHeight : messageEvent.data.height,
      });
    }
  },
  _displayFallbackContent() {
    this.setState({ showStaticContent: true });
  },
  _dismissGuide() {
    GettingStartedActions.dismiss.triggerPromise().then(() => {
      if (this.props.onDismiss) {
        this.props.onDismiss();
      }
    });
  },
  render() {
    let dismissButton = null;
    if (!this.props.noDismissButton) {
      dismissButton = (
        <Button bsStyle="default" bsSize="small" onClick={this._dismissGuide}>
          <i className="fa fa-times" /> Dismiss guide
        </Button>
      );
    }
    let gettingStartedContent = null;
    if (this.state.showStaticContent) {
      gettingStartedContent = (
        <Grid>
          <Row>
            <Col mdPush={3} md={6} className="content content-head text-center" style={{ paddingBottom: '15px' }}>
              <span>
                We could not load the{' '}
                <a target="_blank" href="https://gettingstarted.graylog.org/assets/index.html">Graylog Getting Started Guide</a>.
                Please open it directly with a browser that can access the public internet.
              </span>
            </Col>
          </Row>
        </Grid>
      );
    } else {
      const query = Qs.stringify({
        c: this.props.clusterId,
        o: this.props.masterOs,
        v: this.props.masterVersion,
        m: this.props.noDismissButton,
      });

      const iframeStyles = {
        minHeight: this.state.minHeight,
        height: this.state.minHeight,
        width: '100%',
      };
      // hide iframe if there's no content loaded yet
      if (!this.state.guideLoaded) {
        iframeStyles.display = 'none';
      }

      const url = this.state.guideUrl === '' ? (`${this.props.gettingStartedUrl}?${query}`) : this.state.guideUrl;
      let spinner = null;
      if (!this.state.guideLoaded) {
        spinner = (
          <Grid>
            <Row>
              <Col mdPush={3} md={6} className="content content-head text-center" style={{ paddingBottom: '15px' }}>
                <Spinner text="Loading Graylog Getting started guide ..." />
              </Col>
            </Row>
          </Grid>
        );
      }

      gettingStartedContent = (<div>
        {spinner}
        <iframe src={url}
                style={iframeStyles}
                id="getting-started-frame"
                frameBorder="0"
                scrolling="yes">
          <p>Sorry, no iframes</p>
        </iframe>
      </div>);
    }
    return (
      <div id="react-gettingstarted">
        <div className="pull-right">{dismissButton}</div>
        {gettingStartedContent}
      </div>
    );
  },
});

export default GettingStarted;
