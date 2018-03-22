import PropTypes from 'prop-types';
import React from 'react';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';

import UserNotification from 'util/UserNotification';
import Routes from 'routing/Routes';
import { Link } from 'react-router';
import { Row, Col, Button, DropdownButton, MenuItem, Pagination } from 'react-bootstrap';
import TypeAheadDataFilter from 'components/common/TypeAheadDataFilter';
import ControlledTableList from 'components/common/ControlledTableList';
import ContentPackStatus from 'components/content-packs/ContentPackStatus';
import CombinedProvider from 'injection/CombinedProvider';
import ContentPacksListStyle from './ContentPacksList.css';

const { ContentPacksActions, ContentPacksStore } = CombinedProvider.get('ContentPacks');

const ContentPacksList = createReactClass({
  displayName: 'ContentPacksList',

  propTypes: {
    contentPacks: PropTypes.arrayOf(PropTypes.object),
  },

  mixins: [Reflux.connect(ContentPacksStore)],

  defaultProps: {
    contentPacks: [],
  },

  getInitialState() {
    return {
      filteredContentPacks: this.props.contentPacks,
      pageSize: 10,
      currentPage: 1,
    };
  },

  componentWillReceiveProps(nextProps) {
    this.setState({ filteredContentPacks: nextProps.contentPacks });
  },

  _deleteContentPack(contentPackId) {
    if (window.confirm('You are about to delete this content pack, are you sure?')) {
      ContentPacksActions.delete(contentPackId).then(() => {
        UserNotification.success('Content Pack deleted successfully.', 'Success');
        ContentPacksActions.list();
      }, () => {
        UserNotification.error('Deleting bundle failed, please check your logs for more information.', 'Error');
      });
    }
  },

  _formatItems(items) {
    const begin = (this.state.pageSize * (this.state.currentPage - 1));
    const end = begin + this.state.pageSize;
    const shownItems = items.slice(begin, end);

    return shownItems.map((item) => {
      const states = item.states || [];
      const updateButton = states.includes('updatable') ? <Button bsSize="small" bsStyle="primary">Update</Button> : '';

      return (
        <ControlledTableList.Item key={item.id}>
          <Row className="row-sm">
            <Col md={9}>
              <h3><Link to={Routes.SYSTEM.CONTENTPACKS.show(item.id)}>{item.name}</Link> <small>Version: {item.rev}</small>
                <ContentPackStatus states={states} />
              </h3>
            </Col>
            <Col md={3} className="text-right">
              {updateButton}
              &nbsp;
              <Button bsStyle="info" bsSize="small">Install</Button>
              &nbsp;
              <DropdownButton id={`more-actions-${item.id}`} title="More Actions" bsSize="small" pullRight>
                <MenuItem onSelect={() => { this._deleteContentPack(item.id); }}>Remove all</MenuItem>
                <MenuItem>Download</MenuItem>
              </DropdownButton>
            </Col>
          </Row>
          <Row className="row-sm">
            <Col md={12}>
              {item.summary}&nbsp;
            </Col>
          </Row>
        </ControlledTableList.Item>
      );
    });
  },

  _filterContentPacks(filteredItems) {
    this.setState({ filteredContentPacks: filteredItems });
  },

  _itemsShownChange(event) {
    this.setState({ pageSize: event.target.value });
  },

  _onChangePage(eventKey, event) {
    event.preventDefault();
    const pageNo = Number(eventKey);
    this.setState({ currentPage: pageNo });
  },

  MAX_PAGE_BUTTONS: 10,

  render() {
    const numberPages = Math.ceil(this.state.filteredContentPacks.length / this.state.pageSize);
    const pagination = (<Pagination bsSize="small"
                                    bsStyle={`pagination ${ContentPacksListStyle.pager}`}
                                    items={numberPages}
                                    maxButtons={this.MAX_PAGE_BUTTONS}
                                    activePage={this.state.currentPage}
                                    onSelect={this._onChangePage}
                                    prev
                                    next
                                    first
                                    last />);
    const pageSizeSelector = (<span>Show:&nbsp;
      <select onChange={this._itemsShownChange} value={this.state.pageSize}>
        <option>10</option>
        <option>25</option>
        <option>50</option>
        <option>100</option>
      </select>
    </span>);

    return (
      <div>
        <Row className="row-sm">
          <Col md={5}>
            <TypeAheadDataFilter
              id="content-packs-filter"
              label="Filter"
              data={this.props.contentPacks}
              displayKey="name"
              onDataFiltered={this._filterContentPacks}
              searchInKeys={['name', 'summary']}
              filterSuggestions={[]}
            />
          </Col>
          <Col md={5}>
            {pagination}
          </Col>
          <Col md={2} className="text-right">
            {pageSizeSelector}
          </Col>
        </Row>
        <ControlledTableList>
          <ControlledTableList.Header />
          {this._formatItems(this.state.filteredContentPacks)}
        </ControlledTableList>
        <Row className="row-sm">
          <Col md={5} />
          <Col md={5}>
            {pagination}
          </Col>
          <Col md={2} className="text-right">
            {pageSizeSelector}
          </Col>
        </Row>
      </div>
    );
  },
});

export default ContentPacksList;
