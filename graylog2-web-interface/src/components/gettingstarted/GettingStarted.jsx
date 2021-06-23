/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';
import Qs from 'qs';
import styled, { css } from 'styled-components';

import { Grid, Col, Button } from 'components/graylog';
import { ContentHeadRow, Spinner, Icon } from 'components/common';
import ActionsProvider from 'injection/ActionsProvider';

const GettingStartedActions = ActionsProvider.getActions('GettingStarted');

const Container = styled.div`
  height: 100%;
  display: grid;
  display: -ms-grid;
  grid-template-rows: max-content 1fr;
  -ms-grid-rows: max-content 1fr;
  grid-template-columns: 1fr;
  -ms-grid-columns: 1fr;
`;

const DismissButtonSection = styled.div`
  grid-column: 1;
  -ms-grid-column: 1;
  grid-row: 1;
  -ms-grid-row: 1;
`;

const DismissButton = styled(Button)`
  margin-right: 5px;
  top: -4px;
  position: relative;
`;

const ContentSection = styled.div`
  grid-row: 2;
  -ms-grid-row: 2;
  grid-column: 1;
  -ms-grid-column: 1;
`;

const GettingStartedIframe = styled.iframe(({ hidden }) => css`
  display: ${hidden ? 'none' : 'block'};
  width: 100%;
  height: 100%;
`);

class GettingStarted extends React.Component {
  timeoutId = null;

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

  constructor(props) {
    super(props);

    this.state = {
      guideLoaded: false,
      guideUrl: '',
      showStaticContent: false,
    };
  }

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
    const { noDismissButton, clusterId, masterOs, masterVersion, gettingStartedUrl } = this.props;
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
        o: masterOs,
        v: masterVersion,
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
      <Container>
        <DismissButtonSection>
          <div className="pull-right">{dismissButton}</div>
        </DismissButtonSection>
        <ContentSection>
          {gettingStartedContent}
        </ContentSection>
      </Container>
    );
  }
}

export default GettingStarted;
