'use strict';

var React = require('react');

var BootstrapModal = require('../bootstrap/BootstrapModal');

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
                    Type: {this.props.type.toLowerCase()}, cached for {this.props.cacheTime} seconds.&nbsp;
                    Widget is bound to stream {this.props.config.stream_id}.
                </p>
            );
        } else {
            basicConfigurationMessage = (
                <p>
                    Type: {this.props.type.toLowerCase()}, cached for {this.props.cacheTime} seconds.&nbsp;
                    Widget is <strong>not</strong> bound to a stream.
                </p>
            );
        }

        return basicConfigurationMessage;
    },
    _formatConfigurationKey(key) {
        return key.replace(/_/g, " ");
    },
    _formatConfigurationValue(key, value) {
        return key === "query" && value === "" ? "*" : String(value);
    },
    _getConfigAsDescriptionList() {
        var configKeys = Object.keys(this.props.config);
        if (configKeys.length === 0) {
            return [];
        }
        var configListElements = [];

        configKeys.forEach((key) => {
            if (this.props.config[key] !== null) {
                configListElements.push(<dt key={key}>{this._formatConfigurationKey(key)}:</dt>);
                configListElements.push(
                    <dd key={key + "-value"}>{this._formatConfigurationValue(key, this.props.config[key])}</dd>
                );
            }
        });

        return configListElements;
    },
    render() {
        var configModalHeader = <h2 className="modal-title">Widget "{this.props.title}" configuration</h2>;
        var configModalBody = (
            <div className="configuration">
                {this._getBasicConfiguration()}
                <div>More details:
                    <dl className="dl-horizontal">
                        <dt>Widget ID:</dt>
                        <dd>{this.props.widgetId}</dd>
                        <dt>Dashboard ID:</dt>
                        <dd>{this.props.dashboardId}</dd>
                        <dt>Created by:</dt>
                        <dd>{this.props.creatorUserId}</dd>
                        {this._getConfigAsDescriptionList()}
                    </dl>
                </div>
            </div>
        );

        return (
            <BootstrapModal ref="configModal"
                            onCancel={this.hide}
                            onConfirm={this.props.metricsAction}
                            cancel="Cancel"
                            confirm="Show widget metrics">
               {configModalHeader}
               {configModalBody}
            </BootstrapModal>
        );
    }
});

module.exports = WidgetConfigModal;