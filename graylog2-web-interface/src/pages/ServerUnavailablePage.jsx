import PropTypes from 'prop-types';
import React from 'react';

import { Modal, Well } from 'components/graylog';
import { Icon } from 'components/common';
import DocumentTitle from 'components/common/DocumentTitle';
import DisconnectedThemeStyles from 'theme/styles/disconnectedStyles';

import URLUtils from 'util/URLUtils';

class ServerUnavailablePage extends React.Component {
  static propTypes = {
    server: PropTypes.object,
  };

  static defaultProps = {
    server: undefined,
  };


  state = {
    showDetails: false,
  };

  _toggleDetails = () => {
    this.setState({ showDetails: !this.state.showDetails });
  };

  _formatErrorMessage = () => {
    if (!this.state.showDetails) {
      return null;
    }

    const noInformationMessage = (
      <div>
        <hr />
        <p>There is no information available.</p>
      </div>
    );

    if (!this.props.server || !this.props.server.error) {
      return noInformationMessage;
    }

    const { error } = this.props.server;

    const errorDetails = [];
    if (error.message) {
      errorDetails.push(<dt key="error-title">Error message</dt>, <dd key="error-desc">{error.message}</dd>);
    }
    if (error.originalError) {
      const { originalError } = error;
      errorDetails.push(
        <dt key="status-original-request-title">Original Request</dt>,
        <dd key="status-original-request-content">{String(originalError.method)} {String(originalError.url)}</dd>,
      );
      errorDetails.push(
        <dt key="status-code-title">Status code</dt>,
        <dd key="status-code-desc">{String(originalError.status)}</dd>,
      );

      if (typeof originalError.toString === 'function') {
        errorDetails.push(
          <dt key="full-error-title">Full error message</dt>,
          <dd key="full-error-desc">{originalError.toString()}</dd>,
        );
      }
    }

    if (errorDetails.length === 0) {
      return noInformationMessage;
    }

    return (
      <div>
        <hr style={{ marginTop: 10, marginBottom: 10 }} />
        <p>This is the last response we received from the server:</p>
        <Well bsSize="small" style={{ whiteSpace: 'pre-line' }}>
          <dl style={{ marginBottom: 0 }}>
            {errorDetails}
          </dl>
        </Well>
      </div>
    );
  };

  render() {
    return (
      <DocumentTitle title="Server unavailable">
        <DisconnectedThemeStyles />
        <Modal show>
          <Modal.Header>
            <Modal.Title><Icon name="exclamation-triangle" /> Server currently unavailable</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div>
              <p>
                We are experiencing problems connecting to the Graylog server running on <i>{URLUtils.qualifyUrl('')}</i>.
                Please verify that the server is healthy and working correctly.
              </p>
              <p>You will be automatically redirected to the previous page once we can connect to the server.</p>
              <p>
                Do you need a hand?{' '}
                <a href="https://www.graylog.org/community-support" rel="noopener noreferrer" target="_blank">We can help you</a>.
              </p>
              <div>
                <a role="button" tabIndex={0} onClick={this._toggleDetails}>
                  {this.state.showDetails ? 'Less details' : 'More details'}
                </a>
                {this._formatErrorMessage()}
              </div>
            </div>
          </Modal.Body>
        </Modal>
      </DocumentTitle>
    );
  }
}

export default ServerUnavailablePage;
