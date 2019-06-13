import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'react-bootstrap';
import lodash from 'lodash';

import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';

import styles from './EventDefinitionSummary.css';
import commonStyles from '../common/commonStyles.css';

class EventDefinitionSummary extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
  };

  renderDetails = (eventDefinition) => {
    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>Details</h3>
        <dl>
          <dt>Title</dt>
          <dd>{eventDefinition.title || 'No title given'}</dd>
          <dt>Description</dt>
          <dd>{eventDefinition.description || 'No description given'}</dd>
          <dt>Priority</dt>
          <dd>{lodash.upperFirst(EventDefinitionPriorityEnum.properties[eventDefinition.priority].name)}</dd>
        </dl>
      </React.Fragment>
    );
  };

  renderFilterAndAggregation = (config) => {
    // TODO: Make each type render its own config
    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>Filter & Aggregation</h3>
        {config.type
          ? (
            <dl>
              <dt>Type</dt>
              <dd>{config.type}</dd>
              <dt>Query</dt>
              <dd>{config.query || '*'}</dd>
              <dt>Streams</dt>
              <dd>{config.selected_streams ? config.selected_streams.join(', ') : 'No streams selected'}</dd>
              <dt>Time range</dt>
              <dd>N/A</dd>
            </dl>
          )
          : <p>Not configured.</p>
        }
      </React.Fragment>
    );
  };

  renderField = (fieldName, config, keys) => {
    // TODO: Make each type render its own config
    return (
      <div key={fieldName}>
        <h4>{fieldName}</h4>
        <dl className={styles.innerList}>
          <dt>Is Key?</dt>
          <dd>{keys.includes(fieldName) ? 'Yes' : 'No'}</dd>
          <dt>Data Type</dt>
          <dd>{config.data_type}</dd>
          <dt>Value comes from</dt>
          <dd>{config.providers[0].type}</dd>
          <dt>Template</dt>
          <dd>{config.providers[0].template}</dd>
        </dl>
      </div>
    );
  };

  renderFields = (fields, keys) => {
    const fieldNames = Object.keys(fields);
    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>Fields</h3>
        {fieldNames.length === 0
          ? <p>No Fields configured for Events based on this Definition.</p>
          : fieldNames.map(fieldName => this.renderField(fieldName, fields[fieldName], keys))
        }
      </React.Fragment>
    );
  };

  renderNotifications = (actions) => {
    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>Notifications</h3>
        {actions.length === 0
        && <p>This Event is not configured to trigger any Notifications.</p>}
      </React.Fragment>
    );
  };

  render() {
    const { eventDefinition } = this.props;
    return (
      <Row>
        <Col md={12}>
          <h2 className={commonStyles.title}>Event Summary</h2>
          <Row>
            <Col md={4}>
              {this.renderDetails(eventDefinition)}
            </Col>
            <Col md={4} mdOffset={1}>
              {this.renderFilterAndAggregation(eventDefinition.config)}
            </Col>
          </Row>

          <Row>
            <Col md={4}>
              {this.renderFields(eventDefinition.field_spec, eventDefinition.key_spec)}
            </Col>
            <Col md={4} mdOffset={1}>
              {this.renderNotifications(eventDefinition.actions)}
            </Col>
          </Row>
        </Col>
      </Row>
    );
  }
}

export default EventDefinitionSummary;
