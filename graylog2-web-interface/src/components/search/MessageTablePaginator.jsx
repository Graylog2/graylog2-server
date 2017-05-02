import React, { PropTypes } from 'react';
import ReactDOM from 'react-dom';
import { Page } from 'components/common';

import EventHandlersThrottler from 'util/EventHandlersThrottler';

import StoreProvider from 'injection/StoreProvider';
const SearchStore = StoreProvider.getStore('Search');
const UniversalSearchStore = StoreProvider.getStore('UniversalSearch');

import $ from 'jquery';
global.jQuery = $;
require('bootstrap/js/affix');

const MessageTablePaginator = React.createClass({
  propTypes: {
    resultCount: PropTypes.number.isRequired,
    currentPage: PropTypes.number.isRequired,
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]),
    position: PropTypes.string,
  },
  getInitialState() {
    return {
      paginationWidth: 0,
    };
  },

  componentDidMount() {
    this._setPaginationWidth();
    this._initializeAffix();
    window.addEventListener('resize', this._setPaginationWidth);
  },

  componentWillUnmount() {
    window.removeEventListener('resize', this._setPaginationWidth);
  },

  eventsThrottler: new EventHandlersThrottler(),

  _initializeAffix() {
    if (this.props.position === 'bottom') {
      $(ReactDOM.findDOMNode(this.refs.paginatorAffix)).affix({
        offset: {
          top: 500,
          bottom: 10,
        },
      });
    }
  },
  _setPaginationWidth() {
    if (this.props.position === 'bottom') {
      this.eventsThrottler.throttle(() => {
        this.setState({ paginationWidth: ReactDOM.findDOMNode(this.refs.paginatorContainer).clientWidth });
      });
    }
  },
  _numberOfPages() {
    return Math.ceil(this.props.resultCount / UniversalSearchStore.DEFAULT_LIMIT);
  },
  _minPage() {
    const currentTenMin = Math.floor(this.props.currentPage / 10) * 10;
    return Math.max(1, currentTenMin);
  },
  _maxPage() {
    if (this.props.currentPage > this._numberOfPages()) {
      return this.props.currentPage;
    }
    const currentTenMax = Math.ceil((this.props.currentPage + 1) / 10) * 10;
    return Math.min(this._numberOfPages(), currentTenMax);
  },
  _onPageChanged(page) {
    let newPage;

    if (page === 'Previous') {
      newPage = this.props.currentPage - 1;
    } else if (page === 'Next') {
      newPage = this.props.currentPage + 1;
    } else {
      newPage = Number(page);
    }

    SearchStore.page = newPage;
  },
  render() {
    const pages = [];

    pages.push(<Page key="previous" href="#" page="Previous" isDisabled={this.props.currentPage === 1}
                     onPageChanged={this._onPageChanged} />);
    for (let i = this._minPage(); i <= this._maxPage(); i++) {
      pages.push(<Page key={`page${i}`} href="#" page={i} isActive={i === this.props.currentPage}
                       onPageChanged={this._onPageChanged} />);
    }
    pages.push(<Page key="next" href="#" page="Next" isDisabled={this.props.currentPage >= this._maxPage()}
                     onPageChanged={this._onPageChanged} />);

    const pagination = (
      <ul className="pagination">
        {pages}
      </ul>
    );

    let nav;
    if (this.props.position === 'bottom') {
      nav = (
        <div ref="paginatorAffix">
          {this.props.children}
          <nav className="text-center"
               style={{ width: this.state.paginationWidth + 20 }}>
            {pagination}
          </nav>
        </div>
      );
    } else {
      nav = <nav className="text-center">{pagination}</nav>;
    }

    return (
      <div ref="paginatorContainer" id={`message-table-paginator-${this.props.position}`}>
        {nav}
      </div>
    );
  },
});

export default MessageTablePaginator;
