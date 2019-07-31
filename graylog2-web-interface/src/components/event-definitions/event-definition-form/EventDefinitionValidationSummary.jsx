import React from 'react';
import PropTypes from 'prop-types';
import { Alert, Col, Row } from 'react-bootstrap';

import styles from './EventDefinitionValidationSummary.css';

class EventDefinitionValidationSummary extends React.Component {
  static propTypes = {
    validation: PropTypes.object.isRequired,
  };

  render() {
    const { validation } = this.props;
    const fieldsWithErrors = Object.keys(validation.errors);
    if (fieldsWithErrors.length === 0) {
      return null;
    }

    return (
      <Row>
        <Col md={12}>
          <Alert bsStyle="danger" className={styles.validationSummary}>
            <h4>We found some errors!</h4>
            <p>Please correct the following errors before saving this Event Definition:</p>
            <ul>
              {fieldsWithErrors.map((field) => {
                return validation.errors[field].map(error => <li key={`${field}-${error}`}>{error}</li>);
              })}
            </ul>
          </Alert>
        </Col>
      </Row>
    );
  }
}

export default EventDefinitionValidationSummary;
