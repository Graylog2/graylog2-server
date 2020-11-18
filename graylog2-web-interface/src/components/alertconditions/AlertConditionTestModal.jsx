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

import { BootstrapModalWrapper } from 'components/bootstrap';
import { Spinner, Icon } from 'components/common';
import { Alert, Modal, Button } from 'components/graylog';
import CombinedProvider from 'injection/CombinedProvider';

import style from './AlertConditionTestModal.css';

const { AlertConditionsActions } = CombinedProvider.get('AlertConditions');

class AlertConditionTestModal extends React.Component {
  static propTypes = {
    stream: PropTypes.object.isRequired,
    condition: PropTypes.object.isRequired,
  };

  state = {
    testResults: undefined,
    isTesting: false,
  };

  open = () => {
    this.modal.open();
    this.testCondition();
  };

  close = () => {
    this.modal.close();
  };

  testCondition = () => {
    const { stream, condition } = this.props;

    this.setState({ isTesting: true, testResults: undefined });

    AlertConditionsActions.test(stream.id, condition.id)
      .then(
        (testResults) => this.setState({ testResults: testResults }),
        (error) => {
          if (error.status === 400) {
            // Condition testing failed but we should still get results in the body
            this.setState({ testResults: error.additional.body });

            return;
          }

          // Create a default error message to display in frontend
          this.setState({
            testResults: {
              error: true,
              error_messages: [{
                type: 'Unexpected error',
                message: 'Could not test Condition, please try again or check your server logs for more information.',
              }],
            },
          });
        },
      )
      .finally(() => this.setState({ isTesting: false }));
  };

  renderErroneousCondition = (testResults) => {
    return (
      <span>
        <p><b>There was an error testing the Condition.</b></p>
        <p>
          <ul className={style.errorMessages}>
            {testResults.error_messages.map(({ message, type }) => (
              <li key={`${type}-${message}`}>{message} ({type})</li>
            ))}
          </ul>
        </p>
      </span>
    );
  };

  renderSatisfiedCondition = (testResults) => {
    return (
      <span>
        <Icon name="bell" className={style.testResultIcon} />
        <p className={style.testResultText}>Condition was satisfied and an Alert would be triggered.<br />
          <b>Details</b>: {testResults.description}
        </p>
      </span>
    );
  };

  renderUnsatisfiedCondition = () => {
    return (
      <div>
        <Icon name="bell-slash" className={style.testResultIcon} />
        <p className={style.testResultText}>
          Condition was <b>not</b> satisfied and an Alert would <b>not</b> be triggered.
        </p>
      </div>
    );
  };

  renderTestResults = (testResults) => {
    if (testResults.error) {
      return this.renderErroneousCondition(testResults);
    }

    return testResults.triggered ? this.renderSatisfiedCondition(testResults) : this.renderUnsatisfiedCondition(testResults);
  };

  render() {
    const { condition } = this.props;
    const { isTesting, testResults } = this.state;

    return (
      <BootstrapModalWrapper ref={(c) => { this.modal = c; }} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>Alert Condition <em>{condition.title}</em> test results</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {testResults ? (
            <Alert bsStyle={testResults.error ? 'danger' : 'info'}>
              {this.renderTestResults(testResults)}
            </Alert>
          ) : (
            <Spinner text="Testing alert condition, please wait..." />
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={this.close}>Close</Button>
          <Button bsStyle="primary" onClick={this.testCondition} disabled={isTesting}>
            {isTesting ? 'Testing...' : 'Test again'}
          </Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );
  }
}

export default AlertConditionTestModal;
