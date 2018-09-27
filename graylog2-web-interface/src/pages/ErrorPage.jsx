import React from 'react';
import PropTypes from 'prop-types';
import { Col, Jumbotron, Row } from 'react-bootstrap';
import { DocumentTitle } from 'components/common';

// eslint-disable-next-line import/no-webpack-loader-syntax
import style from '!style/useable!css!./NotFoundPage.css';
import errorPageStyles from './ErrorPage.css';
import SupportSources from '../components/support/SupportSources';

class ErrorPage extends React.Component {
  static propTypes = {
    error: PropTypes.shape({
      message: PropTypes.string.isRequired,
    }).isRequired,
    info: PropTypes.shape({
      componentStack: PropTypes.string.isRequired,
    }).isRequired,
  };

  componentDidMount() {
    style.use();
  }

  componentWillUnmount() {
    style.unuse();
  }

  render() {
    const { error, info } = this.props;

    return (
      <div className="container-fluid">
        <DocumentTitle title="Something went wrong.">
          <Row className="jumbotron-container">
            <Col mdOffset={2} md={8}>
              <Jumbotron>
                <h1>Something went wrong.</h1>
                <p>It seems like the page you navigated to contained an error.</p>
                <p>You can use the navigation to reach other parts of the product, refresh the page or submit an error report.</p>
                <div className={errorPageStyles.errorMessage}>
                  <div className="content" style={{ padding: '2em' }}>
                    <SupportSources />
                  </div>
                  <dl>
                    <dt>Error:</dt>
                    <dd className={errorPageStyles.greyBackground}>
                      <pre className="content">{error.message}</pre>
                    </dd>

                    <dt>Stack:</dt>
                    <dd className={errorPageStyles.greyBackground}>
                      <pre className="content">{error.stack}</pre>
                    </dd>

                    <dt>Component Tree:</dt>
                    <dd className={errorPageStyles.greyBackground}>
                      <pre className="content">{info.componentStack}</pre>
                    </dd>
                  </dl>
                </div>
              </Jumbotron>
            </Col>
          </Row>
        </DocumentTitle>
      </div>
    );
  }
}

export default ErrorPage;
