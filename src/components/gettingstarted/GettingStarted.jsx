import React, { PropTypes } from 'react';
import Spinner from '../common/Spinner';
import Qs from 'qs';

const GettingStarted = React.createClass({
  propTypes() {
    return {
      clusterId: PropTypes.string.isRequired,
      masterOs: PropTypes.string.isRequired,
      masterVersion: PropTypes.string.isRequired,
      gettingStartedUrl: PropTypes.string.isRequired,
    };
  },
  getInitialState() {
    return {
      guideLoaded: false,
      showStaticContent: false,
      frameHeight: '500px',
      minHeight: 200,
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
  render() {
    let gettingStartedContent = null;
    if (this.state.showStaticContent) {
      gettingStartedContent = <div>Could not load dynamic content in time, living with static content for now.</div>;
    } else {
      const query = Qs.stringify({
        c: this.props.clusterId,
        o: this.props.masterOs,
        v: this.props.masterVersion,
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

      const url = this.props.gettingStartedUrl + '?' + query;
      let spinner = null;
      if (!this.state.guideLoaded) {
        spinner = (<div>
          <h1>Welcome to Graylog</h1>
          <Spinner text="Loading Graylog Getting started guide ..."/>
        </div>);
      }

      gettingStartedContent = (<div>
        {spinner}
        <iframe src={url}
                style={iframeStyles}
                id="getting-started-frame"
                frameBorder="0"
                scrolling="no">
          <p>Sorry, no iframes</p>
        </iframe>
      </div>);
    }
    return (<div>
      {gettingStartedContent}
    </div>);
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
        minHeight: messageEvent.data.height === 0 ? this.state.minHeight : messageEvent.data.height,
      });
    }
  },
  _displayFallbackContent() {
    this.setState({showStaticContent: true});
  },
});

module.exports = GettingStarted;
