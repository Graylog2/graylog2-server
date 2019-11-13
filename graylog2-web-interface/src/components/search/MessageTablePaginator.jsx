import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';

import { Pager } from 'components/graylog';

import EventHandlersThrottler from 'util/EventHandlersThrottler';

import StoreProvider from 'injection/StoreProvider';

import $ from 'jquery';

const UniversalSearchStore = StoreProvider.getStore('UniversalSearch');
global.jQuery = $;
require('bootstrap/js/affix');

class MessageTablePaginator extends React.Component {
  eventsThrottler = new EventHandlersThrottler();

  static propTypes = {
    resultCount: PropTypes.number.isRequired,
    currentPage: PropTypes.number.isRequired,
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]),
    onPageChange: PropTypes.func.isRequired,
    pageSize: PropTypes.number,
    position: PropTypes.string,
  };

  static defaultProps = {
    pageSize: UniversalSearchStore.DEFAULT_LIMIT,
    children: undefined,
    position: '',
  };

  constructor(props) {
    super(props);

    this.state = {
      paginationWidth: 0,
    };
  }

  componentDidMount() {
    this._setPaginationWidth();
    this._initializeAffix();
    window.addEventListener('resize', this._setPaginationWidth);
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this._setPaginationWidth);
  }

  _initializeAffix() {
    const { position } = this.props;

    if (position === 'bottom') {
      // eslint-disable-next-line react/no-find-dom-node
      $(ReactDOM.findDOMNode(this.paginatorAffix)).affix({
        offset: {
          top: 500,
          bottom: 10,
        },
      });
    }
  }

  _setPaginationWidth() {
    const { position } = this.props;

    if (position === 'bottom') {
      this.eventsThrottler.throttle(() => {
        // eslint-disable-next-line react/no-find-dom-node
        this.setState({ paginationWidth: ReactDOM.findDOMNode(this.paginatorContainer).clientWidth });
      });
    }
  }

  _numberOfPages() {
    const { resultCount, pageSize } = this.props;

    return Math.ceil(resultCount / pageSize);
  }

  _minPage() {
    const { currentPage } = this.props;

    const currentTenMin = Math.floor(currentPage / 10) * 10;
    return Math.max(1, currentTenMin);
  }

  _maxPage() {
    const { currentPage } = this.props;

    if (currentPage > this._numberOfPages()) {
      return currentPage;
    }

    const currentTenMax = Math.ceil((currentPage + 1) / 10) * 10;
    return Math.min(this._numberOfPages(), currentTenMax);
  }

  _onPageChanged(page) {
    const { currentPage, onPageChange } = this.props;
    let newPage;

    if (page === 'Previous') {
      newPage = currentPage - 1;
    } else if (page === 'Next') {
      newPage = currentPage + 1;
    } else {
      newPage = Number(page);
    }

    onPageChange(newPage);
  }

  render() {
    const { children, currentPage, position } = this.props;
    const { paginationWidth } = this.state;
    const pages = [];

    let nav;

    pages.push(
      <Pager.Item href="#"
                  disabled={currentPage === 1}
                  onSelect={() => this._onPageChanged('Previous')}
                  key="previous">
        Previous
      </Pager.Item>,
    );
    for (let i = this._minPage(); i <= this._maxPage(); i += 1) {
      pages.push(
        <Pager.Item href="#"
                    className={i === currentPage && 'active'}
                    onSelect={() => this._onPageChanged(i)}
                    key={`page${i}`}>
          {i}
        </Pager.Item>,
      );
    }
    pages.push(
      <Pager.Item href="#"
                  onSelect={() => this._onPageChanged('Next')}
                  disabled={currentPage >= this._maxPage()}
                  key="next">
        Next
      </Pager.Item>,
    );

    const pagination = (
      <ul className="pagination">
        {pages}
      </ul>
    );
    if (position === 'bottom') {
      nav = (
        <div ref={(paginatorAffix) => { this.paginatorAffix = paginatorAffix; }}>
          {children}
          <nav className="text-center"
               style={{ width: paginationWidth + 20 }}>
            {pagination}
          </nav>
        </div>
      );
    } else {
      nav = <nav className="text-center">{pagination}</nav>;
    }

    return (
      <div ref={(element) => { this.paginatorContainer = element; }}
           id={`message-table-paginator-${position}`}>
        {nav}
      </div>
    );
  }
}

export default MessageTablePaginator;
