import React from 'react';
import { Modal, Well } from 'react-bootstrap';

import URLUtils from 'util/URLUtils';

import disconnectedStyle from '!style/useable!css!less!stylesheets/disconnected.less';

const ServerUnavailablePage = React.createClass({
  propTypes: {
    server: React.PropTypes.object,
  },

  getInitialState() {
    return {
      showDetails: false,
    };
  },

  componentDidMount() {
    disconnectedStyle.use();
  },

  componentWillUnmount() {
    disconnectedStyle.unuse();
  },

  _toggleDetails() {
    this.setState({showDetails: !this.state.showDetails});
  },

  _formatErrorMessage() {
    if (!this.state.showDetails) {
      return null;
    }

    const noInformationMessage = (
      <div>
        <hr/>
        <p>There is no information available.</p>
      </div>
    );

    if (!this.props.server || !this.props.server.error) {
      return noInformationMessage;
    }

    const error = this.props.server.error;

    const errorDetails = [];
    if (error.message) {
      errorDetails.push(<dt key="error-title">Error message</dt>, <dd key="error-desc">{error.message}</dd>);
    }
    if (error.originalError) {
      errorDetails.push(
        <dt key="status-code-title">Status code</dt>,
        <dd key="status-code-desc">{String(error.originalError.status)}</dd>
      );

      if (typeof error.originalError.toString === 'function') {
        errorDetails.push(
          <dt key="full-error-title">Full error message</dt>,
          <dd key="full-error-desc">{error.originalError.toString()}</dd>
        );
      }
    }

    if (errorDetails.length === 0) {
      return noInformationMessage;
    }

    return (
      <div>
        <hr style={{marginTop: 10, marginBottom: 10}}/>
        <p>This is the last response we received from the server:</p>
        <Well bsSize="small" style={{whiteSpace: 'pre-line'}}>
          <dl style={{marginBottom: 0}}>
            {errorDetails}
          </dl>
        </Well>
      </div>
    );
  },

  render() {
    return (
      <Modal show>
        <Modal.Header>
          <Modal.Title><i className="fa fa-exclamation-triangle"/> Server currently unavailable</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div>
            <p>
              We are experiencing problems connecting to the Graylog server running on <i>{URLUtils.qualifyUrl('')}</i>.
            </p>
            <p>You will be automatically redirected to the previous page once we can connect to the server.</p>
            <p>
              Please verify that the server is healthy and it is working correctly. Do you need a hand?{' '}
              <a href="https://www.graylog.org/community-support" target="_blank">We can help you</a>.
            </p>
            <div>
              <a href="#" onClick={this._toggleDetails}>
                {this.state.showDetails ? 'Less details' : 'More details'}
              </a>
              {this._formatErrorMessage()}
            </div>
          </div>
        </Modal.Body>
      </Modal>
    );
  },
});

export default ServerUnavailablePage;
