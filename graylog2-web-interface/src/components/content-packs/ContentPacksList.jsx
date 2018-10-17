import PropTypes from 'prop-types';
import React from 'react';

import Routes from 'routing/Routes';
import { Link } from 'react-router';
import { Row, Col, Button, DropdownButton, MenuItem, Pagination, Modal, ButtonToolbar } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import TypeAheadDataFilter from 'components/common/TypeAheadDataFilter';

import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import ControlledTableList from 'components/common/ControlledTableList';
import ContentPackStatus from 'components/content-packs/ContentPackStatus';
import ContentPackDownloadControl from 'components/content-packs/ContentPackDownloadControl';
import ContentPacksListStyle from './ContentPacksList.css';
import ContentPackInstall from './ContentPackInstall';

class ContentPacksList extends React.Component {
  static propTypes = {
    contentPacks: PropTypes.arrayOf(PropTypes.object),
    contentPackMetadata: PropTypes.object,
    onDeletePack: PropTypes.func,
    onInstall: PropTypes.func,
  };

  static defaultProps = {
    contentPacks: [],
    onDeletePack: () => {},
    onInstall: () => {},
    contentPackMetadata: {},
  };

  constructor(props) {
    super(props);

    this.state = {
      filteredContentPacks: this.props.contentPacks,
      pageSize: 10,
      currentPage: 1,
    };

    this._filterContentPacks = this._filterContentPacks.bind(this);
    this._itemsShownChange = this._itemsShownChange.bind(this);
    this._onChangePage = this._onChangePage.bind(this);
  }

  componentWillReceiveProps(nextProps) {
    this.setState({ filteredContentPacks: nextProps.contentPacks });
  }

  _installModal(item) {
    let modalRef;
    let installRef;

    const closeModal = () => {
      modalRef.close();
    };

    const open = () => {
      modalRef.open();
    };

    const onInstall = () => {
      installRef.onInstall();
      modalRef.close();
    };

    const modal = (
      <BootstrapModalWrapper ref={(node) => { modalRef = node; }} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>Install</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ContentPackInstall ref={(node) => { installRef = node; }}
                              contentPack={item}
                              onInstall={this.props.onInstall} />
        </Modal.Body>
        <Modal.Footer>
          <div className="pull-right">
            <ButtonToolbar>
              <Button bsStyle="primary" onClick={onInstall}>Install</Button>
              <Button onClick={closeModal}>Close</Button>
            </ButtonToolbar>
          </div>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );

    return { openFunc: open, installModal: modal };
  }

  _formatItems(items) {
    const begin = (this.state.pageSize * (this.state.currentPage - 1));
    const end = begin + this.state.pageSize;
    const shownItems = items.slice(begin, end);

    return shownItems.map((item) => {
      const { openFunc, installModal } = this._installModal(item);
      let downloadRef;
      const downloadModal = (<ContentPackDownloadControl
        ref={(node) => { downloadRef = node; }}
        contentPackId={item.id}
        revision={item.rev}
      />);

      const metadata = this.props.contentPackMetadata[item.id] || {};
      const installed = Object.keys(metadata).find(rev => metadata[rev].installation_count > 0);
      const states = installed ? ['installed'] : [];
      const updateButton = states.includes('updatable') ? <Button bsSize="small" bsStyle="primary">Update</Button> : '';

      return (
        <ControlledTableList.Item key={item.id}>
          <Row className="row-sm">
            <Col md={9}>
              <h3><Link to={Routes.SYSTEM.CONTENTPACKS.show(item.id)}>{item.name}</Link> <small>Latest Version: {item.rev} <ContentPackStatus contentPackId={item.id} states={states} /> </small>
              </h3>
            </Col>
            <Col md={3} className="text-right">
              {updateButton}
              &nbsp;
              <Button bsStyle="info" bsSize="small" onClick={openFunc}>Install</Button>
              {installModal}
              &nbsp;
              <DropdownButton id={`more-actions-${item.id}`} title="More Actions" bsSize="small" pullRight>
                <MenuItem onSelect={() => { this.props.onDeletePack(item.id); }}>Delete All Versions</MenuItem>
                <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.edit(encodeURIComponent(item.id), encodeURIComponent(item.rev))}>
                  <MenuItem>Create New Version</MenuItem>
                </LinkContainer>
                <MenuItem onSelect={() => { downloadRef.open(); }}>Download</MenuItem>
              </DropdownButton>
              {downloadModal}
            </Col>
          </Row>
          <Row className="row-sm content-packs-summary">
            <Col md={12}>
              {item.summary}&nbsp;
            </Col>
          </Row>
        </ControlledTableList.Item>
      );
    });
  }

  _filterContentPacks(filteredItems) {
    this.setState({ filteredContentPacks: filteredItems });
  }

  _itemsShownChange(event) {
    this.setState({ pageSize: event.target.value });
  }

  _onChangePage(eventKey, event) {
    event.preventDefault();
    const pageNo = Number(eventKey);
    this.setState({ currentPage: pageNo });
  }

  MAX_PAGE_BUTTONS = 10;

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

    const noContentMessage = this.props.contentPacks.length <= 0 ?
      'No content packs found. Please create or upload one' :
      'No matching content packs found';
    const content = this.state.filteredContentPacks.length <= 0 ?
      (<div>{noContentMessage}</div>) :
      (<ControlledTableList>
        <ControlledTableList.Header />
        {this._formatItems(this.state.filteredContentPacks)}
      </ControlledTableList>);

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
        {content}
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
  }
}

export default ContentPacksList;
