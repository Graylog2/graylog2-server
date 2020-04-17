import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import AppConfig from 'util/AppConfig';
import NotFoundBackgroundImage from 'assets/not-found-bg.jpg';

import { Icon, ReportedErrorDetails } from 'components/common';
import { Button } from 'components/graylog';
import SupportSources from 'components/support/SupportSources';
import ClipboardButton from 'components/common/ClipboardButton';

const ToggleDetails = styled.div`
  font-weight: normal;
`;

class RuntimeErrorPage extends React.Component {
  static propTypes = {
    error: PropTypes.shape({
      message: PropTypes.string.isRequired,
      stack: PropTypes.string,
    }).isRequired,
    componentStack: PropTypes.string.isRequired,
  };

  constructor(props) {
    super(props);
    this.state = {
      showDetails: AppConfig.gl2DevMode(),
    };
  }

  _toggleDetails = (e) => {
    e.preventDefault();
    this.setState(({ showDetails }) => ({ showDetails: !showDetails }));
  };

  render() {
    const { error, componentStack } = this.props;
    const { showDetails } = this.state;
    const errorDetails = `\n\nStack Trace:\n\n${error.stack}\n\nComponent Stack:\n${componentStack}`;
    const description = (
      <>
        <p>It seems like the page you navigated to contained an error.</p>
        <p>You can use the navigation to reach other parts of the product, refresh the page or submit an error report.</p>
      </>
    );

    return (
      <ReportedErrorDetails title="Something went wrong." description={description} backgroundImage={NotFoundBackgroundImage}>
        <div className="content" style={{ padding: '2em' }}>
          <SupportSources />
        </div>
        <dl>
          <dt>
            Error:
            <ToggleDetails className="pull-right">
              <Button bsStyle="link" tabIndex={0} onClick={this._toggleDetails}>
                {showDetails ? 'Show less' : 'Show more'}
              </Button>
            </ToggleDetails>
          </dt>
          <dt>
            <pre className="content">
              <div className="pull-right">
                <ClipboardButton title={<Icon name="copy" fixedWidth />}
                                 bsSize="sm"
                                 text={`${error.message}\n${errorDetails}`}
                                 buttonTitle="Copy error details to clipboard" />
              </div>
              {error.message}
              {showDetails && errorDetails}
            </pre>
          </dt>
        </dl>
      </ReportedErrorDetails>
    );
  }
}

export default RuntimeErrorPage;
