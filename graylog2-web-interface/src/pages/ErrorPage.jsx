import React from 'react';
import PropTypes from 'prop-types';

import { DocumentTitle, Icon } from 'components/common';
import { Button } from 'components/graylog';

// eslint-disable-next-line import/no-webpack-loader-syntax
import errorPageStyles from './ErrorPage.css';

import ErrorJumbotron, { H1 } from './ErrorJumbotron';
import SupportSources from '../components/support/SupportSources';
import ClipboardButton from '../components/common/ClipboardButton';
import AppConfig from '../util/AppConfig';

class ErrorPage extends React.Component {
  static propTypes = {
    error: PropTypes.shape({
      message: PropTypes.string.isRequired,
      stack: PropTypes.string,
    }).isRequired,
    info: PropTypes.shape({
      componentStack: PropTypes.string.isRequired,
    }).isRequired,
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
    const { error, info } = this.props;
    const { showDetails } = this.state;

    const errorDetails = `\n\nStack Trace:\n\n${error.stack}\n\nComponent Stack:\n${info.componentStack}`;

    /* eslint-disable jsx-a11y/href-no-hash */
    return (
      <div className="container-fluid">
        <DocumentTitle title="Something went wrong.">
          <ErrorJumbotron>
            <H1>Something went wrong.</H1>
            <p>It seems like the page you navigated to contained an error.</p>
            <p>You can use the navigation to reach other parts of the product, refresh the page or submit an error report.</p>
            <div className={errorPageStyles.errorMessage}>
              <div className="content" style={{ padding: '2em' }}>
                <SupportSources />
              </div>
              <dl>
                <dt>
                  Error:
                  <div className={`pull-right ${errorPageStyles.toggleDetails}`}>
                    <Button bsStyle="link" tabIndex={0} onClick={this._toggleDetails}>
                      {showDetails ? 'Show less' : 'Show more'}
                    </Button>
                  </div>
                </dt>
                <dd className={errorPageStyles.greyBackground}>
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
                </dd>
              </dl>
            </div>
          </ErrorJumbotron>
        </DocumentTitle>
      </div>
    );
    /* eslint-enable jsx-a11y/href-no-hash */
  }
}

export default ErrorPage;
