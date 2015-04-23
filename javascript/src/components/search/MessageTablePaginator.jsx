'use strict';

var $ = require('jquery');

var React = require('react');
var PageItem = require('react-bootstrap').PageItem;
var ButtonGroup = require('react-bootstrap').ButtonGroup;
var Button = require('react-bootstrap').Button;

var MessageTablePaginator = React.createClass({
    getInitialState() {
        return {
            paginationWidth: 0
        };
    },
    componentDidMount() {
        this._setPaginationWidth();
        $(window).on('resize', this._setPaginationWidth);
    },
    componentWillUnmount() {
        $(window).off('resize', this._setPaginationWidth);
    },
    _setPaginationWidth() {
        if (this.props.position === 'bottom') {
            this.setState({paginationWidth: React.findDOMNode(this.refs.paginatorContainer).clientWidth});
        }
    },
    render() {
        var pages = (
            <ul className="pagination">
                <PageItem href="#">Previous</PageItem>
                <PageItem href="#">1</PageItem>
                <PageItem href="#">Next</PageItem>
            </ul>
        );

        var nav;
        if (this.props.position === 'bottom') {
            nav = (
                <div data-spy="affix"
                     data-offset-top="450"
                     data-offset-bottom="10">
                    {this.props.children}
                    <nav className="text-center"
                     style={{width: this.state.paginationWidth}}>
                    {pages}
                </nav>
                </div>
            );
        } else {
            nav = <nav className='text-center'>{pages}</nav>;
        }

        return (
            <div ref="paginatorContainer" id={"message-table-paginator-" + this.props.position }>
                {nav}
            </div>
        );
    }
});

module.exports = MessageTablePaginator;