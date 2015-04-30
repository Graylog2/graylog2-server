/* global jsRoutes */
'use strict';

var React = require('react/addons');
var UsersStore = require('../../stores/users/UsersStore');

var UserLink = React.createClass({
    getInitialState() {
        return {
            username: this.props.username
        };
    },
    loadData() {
        UsersStore.load(this.state.username).done((user) => {
            this.setState({user: user});
        });
    },
    componentWillReceiveProps(props) {
        this.setState(props);
    },
    componentDidMount() {
        this.loadData();
    },
    render() {
        if (this.state.user) {
            var username = this.state.username;
            var user = this.state.user;
            return (
                <a href={jsRoutes.controllers.UsersController.show(username).url}><i
                    className="fa fa-user"></i> {user.full_name}</a>
            );
        } else {
            return (<span><i className="fa fa-spin fa-spinner"></i> Loading user</span>);
        }
    }
});

module.exports = UserLink;
