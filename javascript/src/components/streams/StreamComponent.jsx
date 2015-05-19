'use strict';

var React = require('react/addons');
var StreamsStore = require('../../stores/streams/StreamsStore');
var StreamList = require('./StreamList');
var CreateStreamButton = require('./CreateStreamButton');
var SupportLink = require('../support/SupportLink');
var PermissionsMixin = require('../../util/PermissionsMixin');
var StreamRulesStore = require('../../stores/streams/StreamRulesStore');
var UsersStore = require('../../stores/users/UsersStore');
var Col = require('react-bootstrap').Col;
var UserNotification = require('../../util/UserNotification');

var StreamComponent = React.createClass({
    mixins: [PermissionsMixin],
    getInitialState() {
        return {
            streams: []
        };
    },
    componentDidMount() {
        this.loadData();
        StreamRulesStore.types((types) => {
            this.setState({streamRuleTypes: types});
        });
        UsersStore.load(this.props.username).done((user) => {
            this.setState({user: user});
        });
        StreamsStore.onChange(this.loadData);
        StreamRulesStore.onChange(this.loadData);
    },
    loadData() {
        StreamsStore.load((streams) => {
            this.setState({streams: streams});
        });
    },
    _onSave(streamId, stream) {
        StreamsStore.save(stream, () => {
            UserNotification.success("Stream has been successfully created!", "Success!");
        });
    },
    render() {
        var createStreamButton = (this.isPermitted(this.props.permissions, ["streams:create"]) ? <CreateStreamButton ref='createStreamButton' onSave={this._onSave} /> : "");
        if (this.state.streams && this.state.streamRuleTypes) {
            return (
                <div>
                    <div className="row content content-head">
                        <Col md={10}>
                            <h1>Streams</h1>

                            <p className="description">
                                You can route incoming messages into streams by applying rules against them. If a
                                message
                                matches all rules of a stream it is routed into it. A message can be routed into
                                multiple
                                streams. You can for example create a stream that contains all SSH logins and configure
                                to be alerted whenever there are more logins than usual.

                                Read more about streams in <a
                                href="http://docs.graylog.org/en/latest/pages/streams.html" target="_blank">the
                                documentation</a>.
                            </p>

                            <SupportLink>
                                Take a look at the
                                {' '}<a href="http://docs.graylog.org/en/latest/pages/external_dashboards.html"
                                   target="_blank">Graylog stream dashboards</a>{' '}
                                for wall-mounted displays or other integrations.
                            </SupportLink>
                        </Col>

                        {createStreamButton}
                    </div>

                    <div className="row content">
                        <Col md={12}>
                            <StreamList streams={this.state.streams} streamRuleTypes={this.state.streamRuleTypes}
                                        permissions={this.props.permissions} user={this.state.user}/>
                        </Col>
                    </div>
                </div>
            );
        } else {
            return (<div><i className="fa fa-spin fa-spinner"/> Loading</div>);
        }
    }
});

module.exports = StreamComponent;
