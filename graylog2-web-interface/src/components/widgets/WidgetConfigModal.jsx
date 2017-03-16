import React, { PropTypes } from 'react';
import { Button, Modal } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import StringUtils from 'util/StringUtils';

import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

const WidgetConfigModal = React.createClass({
  propTypes: {
    boundToStream: PropTypes.bool.isRequired,
    widget: PropTypes.object.isRequired,
    dashboardId: PropTypes.string.isRequired,
  },

  open() {
    this.refs.configModal.open();
  },
  hide() {
    this.refs.configModal.close();
  },
  _getBasicConfiguration() {
    let basicConfigurationMessage;
    const widgetPlugin = PluginStore.exports('widgets').filter(widget => widget.type.toUpperCase() === this.props.widget.type.toUpperCase())[0];
    const widgetType = (widgetPlugin ? widgetPlugin.displayName : 'Not available');
    if (this.props.boundToStream) {
      basicConfigurationMessage = (
        <p>
          Type: {widgetType}, cached for {this.props.widget.cache_time} seconds.&nbsp;
          Widget is bound to stream {this.props.widget.config.stream_id}.
        </p>
      );
    } else {
      basicConfigurationMessage = (
        <p>
          Type: {widgetType}, cached for {this.props.widget.cache_time} seconds.&nbsp;
          Widget is <strong>not</strong> bound to a stream.
        </p>
      );
    }

    return basicConfigurationMessage;
  },
  _formatConfigurationKey(key) {
    return StringUtils.capitalizeFirstLetter(key.replace(/_/g, ' '));
  },
  _formatConfigurationValue(key, value) {
    if (key === 'query' && value === '') {
      return '*';
    }

    if (typeof value === 'string') {
      return String(value);
    }

    if (typeof value === 'object' || typeof value === 'boolean') {
      return JSON.stringify(value, null, 1);
    }

    return value;
  },
  _getConfigAsDescriptionList() {
    const configKeys = Object.keys(this.props.widget.config);
    if (configKeys.length === 0) {
      return [];
    }
    const configListElements = [];

    configKeys.forEach((key) => {
      if (this.props.widget.config[key] !== null) {
        configListElements.push(<dt key={key}>{this._formatConfigurationKey(key)}:</dt>);
        configListElements.push(
          <dd key={`${key}-value`}>{this._formatConfigurationValue(key, this.props.widget.config[key])}</dd>,
        );
      }
    });

    return configListElements;
  },
  render() {
    return (
      <BootstrapModalWrapper ref="configModal">
        <Modal.Header closeButton>
          <Modal.Title><span>Widget <em>{this.props.widget.description}</em> configuration</span></Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className="configuration">
            {this._getBasicConfiguration()}
            <div>More details:
              <dl className="dl-horizontal">
                <dt>Widget ID:</dt>
                <dd>{this.props.widget.id}</dd>
                <dt>Dashboard ID:</dt>
                <dd>{this.props.dashboardId}</dd>
                {this._getConfigAsDescriptionList()}
              </dl>
            </div>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button type="button" onClick={this.hide}>Close</Button>
          <LinkContainer to={Routes.filtered_metrics('master', `org.graylog2.dashboards.widgets.*.${this.props.widget.id}`)}>
            <Button type="button" bsStyle="info">Show widget metrics</Button>
          </LinkContainer>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );
  },
});

export default WidgetConfigModal;
