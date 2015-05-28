'use strict';

var React = require('react/addons');
var Button = require('react-bootstrap').Button;
var ButtonGroup = require('react-bootstrap').ButtonGroup;

var Paginator = React.createClass({
    getDefaultProps() {
        return {
            size: 10,
            currentPage: 1
        };
    },
    _onClick(pageNo) {
        this.props.onSelected(pageNo);
    },
    _onPrevious() {
        var currentPage = this.props.currentPage;
        if (currentPage > 1) {
            this.props.onSelected(currentPage - 1);
        }
    },
    _onNext() {
        var currentPage = this.props.currentPage;
        if (currentPage < this.props.pages) {
            this.props.onSelected(currentPage + 1);
        }
    },
    _leftSize(size) {
        return Math.ceil((size-3)/2);
    },
    _rightSize(size) {
        return Math.floor((size-3)/2);
    },
    _isInMiddle(pageNo, size, pages) {
        return (pageNo > this._leftSize(size)+1 && pageNo < (pages-this._rightSize(size)-1));
    },
    _lastLeftPage(size, currentPage, pages) {
        if (this._isInMiddle(currentPage, size, pages)) {
            return this._leftSize(size);
        } else {
            return this._leftSize(size) + 1;
        }
    },
    _firstRightPage(size, currentPage, pages) {
        if (size > pages) {
            return pages;
        }
        if (this._isInMiddle(currentPage, size, pages)) {
            return (pages-this._rightSize(size));
        } else {
            return (pages-this._rightSize(size)-1);
        }
    },
    _button(i) {
        return <Button key={"page-"+i} onClick={this._onClick.bind(this, i)} disabled={this.props.currentPage === i}>{i}</Button>;
    },
    _filler(key) {
        return <Button key={key} disabled={true}>..</Button>;
    },
    render() {
        var pages = this.props.pages;
        var size = this.props.size;
        var buttons = [];

        var currentPage = this.props.currentPage;

        buttons.push(<Button key="previous" onClick={this._onPrevious} disabled={currentPage === 1}>Previous</Button>);

        if (size < pages) {
            for (var i = 1; i <= this._lastLeftPage(size, currentPage, pages); i++) {
                buttons.push(this._button(i));
            }
            if (this._isInMiddle(currentPage, size, pages)) {
                buttons.push(this._filler("filler-left"));
                buttons.push(this._button(currentPage));
            }
            buttons.push(this._filler('filler-right'));
            for (var n = this._firstRightPage(size, currentPage, pages); n <= pages; n++) {
                buttons.push(this._button(n));
            }
        } else {
            for (var j = 1; j <= pages; j++) {
                buttons.push(this._button(j));
            }
        }
        buttons.push(<Button key="next" onClick={this._onNext} disabled={currentPage === this.props.pages}>Next</Button>);

        return (
            <ButtonGroup>
                {buttons}
            </ButtonGroup>
        );
    }
});

module.exports = Paginator;
