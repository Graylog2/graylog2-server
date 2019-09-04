import React from 'react';
import PropTypes from 'prop-types';
import Qs from 'qs';

import { Grid, Row, Col, Button, Icon } from 'components/graylog';
import { Spinner } from 'components/common';
import ActionsProvider from 'injection/ActionsProvider';

const GettingStartedActions = ActionsProvider.getActions('GettingStarted');

class GettingStarted extends React.Component {
  static propTypes = {
    clusterId: PropTypes.string.isRequired,
    masterOs: PropTypes.string.isRequired,
    masterVersion: PropTypes.string.isRequired,
    gettingStartedUrl: PropTypes.string.isRequired,
    noDismissButton: PropTypes.bool,
    onDismiss: PropTypes.func,
  }

  static defaultProps = {
    noDismissButton: false,
    onDismiss: () => {},
  }

  state = {
    guideLoaded: false,
    guideUrl: '',
    showStaticContent: false,
  };

  timeoutId = null;

  componentDidMount() {
    if (window.addEventListener) {
      window.addEventListener('message', this._onMessage);
    }
    this.timeoutId = window.setTimeout(this._displayFallbackContent, 3000);
  }

  componentWillUnmount() {
    if (window.removeEventListener) {
      window.removeEventListener('message', this._onMessage);
    }
    if (this.timeoutId !== null) {
      window.clearTimeout(Number(this.timeoutId));
      this.timeoutId = null;
    }
  }

  _onMessage = (messageEvent) => {
    const { gettingStartedUrl } = this.props;
    const { minHeight } = this.state;
    // make sure we only process messages from the getting started url, otherwise this can interfere with other messages being posted
    if (gettingStartedUrl.indexOf(messageEvent.origin) === 0) {
      if (this.timeoutId !== null) {
        window.clearTimeout(Number(this.timeoutId));
        this.timeoutId = null;
      }
      this.setState({
        guideLoaded: messageEvent.data.guideLoaded,
        guideUrl: messageEvent.data.guideUrl,
        minHeight: messageEvent.data.height === 0 ? minHeight : messageEvent.data.height,
      });
    }
  };

  _displayFallbackContent = () => {
    this.setState({ showStaticContent: true });
  };

  _dismissGuide = () => {
    const { onDismiss } = this.props;
    GettingStartedActions.dismiss.triggerPromise().then(() => {
      if (onDismiss) {
        onDismiss();
      }
    });
  };

  render() {
    const { noDismissButton, clusterId, masterOs, masterVersion, gettingStartedUrl } = this.props;
    const { minHeight, showStaticContent, guideLoaded, guideUrl } = this.state;

    let dismissButton = null;
    if (!noDismissButton) {
      dismissButton = (
        <Button bsStyle="default" bsSize="small" onClick={this._dismissGuide}>
          <Icon className="fa fa-times" /> Dismiss guide
        </Button>
      );
    }
    let gettingStartedContent = null;
    if (showStaticContent) {
      gettingStartedContent = (
        <Grid>
          <Row>
            <Col mdPush={3} md={6} className="content content-head text-center" style={{ paddingBottom: '15px' }}>
              <span>
                We could not load the{' '}
                <a target="_blank" rel="noopener noreferrer" href="https://gettingstarted.graylog.org/assets/index.html">Graylog Getting Started Guide</a>.
                Please open it directly with a browser that can access the public internet.
              </span>
            </Col>
          </Row>
        </Grid>
      );
    } else {
      const query = Qs.stringify({
        c: clusterId,
        o: masterOs,
        v: masterVersion,
        m: noDismissButton,
      });

      const iframeStyles = {
        minHeight: minHeight,
        height: minHeight,
        width: '100%',
      };
      // hide iframe if there's no content loaded yet
      if (!guideLoaded) {
        iframeStyles.display = 'none';
      }

      const url = guideUrl === '' ? (`${gettingStartedUrl}?${query}`) : guideUrl;
      let spinner = null;
      if (!guideLoaded) {
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

      gettingStartedContent = (
        <div>
          {spinner}
          <iframe src={url}
                  style={iframeStyles}
                  id="getting-started-frame"
                  frameBorder="0"
                  scrolling="yes"
                  title="getting-started-content">
            <p>Sorry, no iframes</p>
          </iframe>
        </div>
      );
    }
    return (
      <div id="react-gettingstarted">
        <div className="pull-right">{dismissButton}</div>
        {gettingStartedContent}
      </div>
    );
  }
}

export default GettingStarted;
