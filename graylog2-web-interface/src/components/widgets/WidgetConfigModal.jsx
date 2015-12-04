'use strict';

var React = require('react');
var Modal = require('react-bootstrap').Modal;
var Button = require('react-bootstrap').Button;

var BootstrapModalWrapper = require('../bootstrap/BootstrapModalWrapper');
var StringUtils = require('../../util/StringUtils');

var WidgetConfigModal = React.createClass({
    open() {
        this.refs.configModal.open();
    },
    hide() {
        this.refs.configModal.close();
    },
    _getBasicConfiguration() {
        var basicConfigurationMessage;
        if (this.props.boundToStream) {
            basicConfigurationMessage = (
                <p>
                    Type: {this.props.widget.type.toLowerCase()}, cached for {this.props.widget.cacheTime} seconds.&nbsp;
                    Widget is bound to stream {this.props.widget.config.stream_id}.
                </p>
            );
        } else {
            basicConfigurationMessage = (
                <p>
                    Type: {this.props.widget.type.toLowerCase()}, cached for {this.props.widget.cacheTime} seconds.&nbsp;
                    Widget is <strong>not</strong> bound to a stream.
                </p>
            );
        }

        return basicConfigurationMessage;
    },
    _formatConfigurationKey(key) {
        return StringUtils.capitalizeFirstLetter(key.replace(/_/g, " "));
    },
    _formatConfigurationValue(key, value) {
        if (key === "query" && value === "") {
            return "*";
        }

        if (typeof value === "string") {
            return String(value);
        }

        if (typeof value === "object") {
            return JSON.stringify(value, null, 1);
        }

        return value;
    },
    _getConfigAsDescriptionList() {
        var configKeys = Object.keys(this.props.widget.config);
        if (configKeys.length === 0) {
            return [];
        }
        var configListElements = [];

        configKeys.forEach((key) => {
            if (this.props.widget.config[key] !== null) {
                configListElements.push(<dt key={key}>{this._formatConfigurationKey(key)}:</dt>);
                configListElements.push(
                    <dd key={key + "-value"}>{this._formatConfigurationValue(key, this.props.widget.config[key])}</dd>
                );
            }
        });

        return configListElements;
    },
    render() {
        return (
            <BootstrapModalWrapper ref="configModal">
                <Modal.Header closeButton>
                    <Modal.Title>{`Widget "${this.props.widget.title}" configuration`}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div className="configuration">
                        {this._getBasicConfiguration()}
                        <div>More details:
                            <dl className="dl-horizontal">
                                <dt>Widget ID:</dt>
                                <dd>{this.props.widget.widgetId}</dd>
                                <dt>Dashboard ID:</dt>
                                <dd>{this.props.widget.dashboardId}</dd>
                                {this._getConfigAsDescriptionList()}
                            </dl>
                        </div>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="button" onClick={this.hide}>Close</Button>
                    <Button type="button" bsStyle="info" onClick={this.props.metricsAction}>Show widget metrics</Button>
                </Modal.Footer>
            </BootstrapModalWrapper>
        );
    }
});

module.exports = WidgetConfigModal;