import React from 'react';
import PropTypes from 'prop-types';
import Qs from 'qs';
import styled, { css } from 'styled-components';

import { Grid, Row, Col, Button } from 'components/graylog';
import { ContentHeadRow, Spinner, Icon } from 'components/common';
import ActionsProvider from 'injection/ActionsProvider';

const GettingStartedActions = ActionsProvider.getActions('GettingStarted');

const FullHeightContainer = styled.div`
  height: calc(100vh - 100px);
  margin-left: -15px;
  margin-right: -15px;
`;

const GettingStartedIframe = styled.iframe(({ hidden }) => css`
  width: 100%;
  display: ${hidden ? 'none' : 'block'};
  min-height: calc(100vh - 100px);
`);

const DismissButton = styled(Button)`
  margin-right: 5px;
  top: -4px;
  position: relative;
`;

class GettingStarted extends React.Component {
  timeoutId = null;

  static propTypes = {
    clusterId: PropTypes.string.isRequired,
    parentOs: PropTypes.string.isRequired,
    parentVersion: PropTypes.string.isRequired,
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
    // make sure we only process messages from the getting started url, otherwise this can interfere with other messages being posted
    if (gettingStartedUrl.indexOf(messageEvent.origin) === 0) {
      if (this.timeoutId !== null) {
        window.clearTimeout(Number(this.timeoutId));
        this.timeoutId = null;
      }
      this.setState({
        guideLoaded: messageEvent.data.guideLoaded,
        guideUrl: messageEvent.data.guideUrl,
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
    const { noDismissButton, clusterId, parentOs, parentVersion, gettingStartedUrl } = this.props;
    const { showStaticContent, guideLoaded, guideUrl } = this.state;

    let dismissButton = null;
    if (!noDismissButton) {
      dismissButton = (
        <DismissButton bsStyle="default" bsSize="small" onClick={this._dismissGuide}>
          <Icon name="times" /> Dismiss guide
        </DismissButton>
      );
    }
    let gettingStartedContent = null;
    if (showStaticContent) {
      gettingStartedContent = (
        <Grid>
          <ContentHeadRow className="content">
            <Col mdPush={3} md={6} className="text-center" style={{ paddingBottom: '15px' }}>
              <span>
                We could not load the{' '}
                <a target="_blank" rel="noopener noreferrer" href="https://gettingstarted.graylog.org/assets/index.html">Graylog Getting Started Guide</a>.
                Please open it directly with a browser that can access the public internet.
              </span>
            </Col>
          </ContentHeadRow>
        </Grid>
      );
    } else {
      const query = Qs.stringify({
        c: clusterId,
        o: parentOs,
        v: parentVersion,
        m: noDismissButton,
      });

      const url = guideUrl === '' ? (`${gettingStartedUrl}?${query}`) : guideUrl;
      let spinner = null;
      if (!guideLoaded) {
        spinner = (
          <Grid>
            <ContentHeadRow className="content">
              <Col mdPush={3} md={6} className="text-center" style={{ paddingBottom: '15px' }}>
                <Spinner text="Loading Graylog Getting started guide ..." />
              </Col>
            </ContentHeadRow>
          </Grid>
        );
      }

      gettingStartedContent = (
        <>
          {spinner}
          <GettingStartedIframe src={url}
                                hidden={!guideLoaded}
                                id="getting-started-frame"
                                frameBorder="0"
                                scrolling="yes"
                                title="getting-started-content">
            <p>Sorry, no iframes</p>
          </GettingStartedIframe>
        </>
      );
    }
    return (
      <FullHeightContainer>
        <div className="pull-right">{dismissButton}</div>
        {gettingStartedContent}
      </FullHeightContainer>
    );
  }
}

export default GettingStarted;
