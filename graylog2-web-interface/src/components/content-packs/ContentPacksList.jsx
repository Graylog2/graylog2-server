import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { Button, Row, Col, DropdownButton, MenuItem, Pagination, Modal, ButtonToolbar } from 'components/graylog';
import TypeAheadDataFilter from 'components/common/TypeAheadDataFilter';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import ControlledTableList from 'components/common/ControlledTableList';
import ContentPackStatus from 'components/content-packs/ContentPackStatus';
import ContentPackDownloadControl from 'components/content-packs/ContentPackDownloadControl';

import ContentPackInstall from './ContentPackInstall';
import ContentPacksListStyle from './ContentPacksList.css';

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

  MAX_PAGE_BUTTONS = 10;

  constructor(props) {
    super(props);

    this.state = {
      filteredContentPacks: props.contentPacks,
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

    const { onInstall: onInstallProp } = this.props;

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
                              onInstall={onInstallProp} />
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
    const { pageSize, currentPage } = this.state;
    const { contentPackMetadata, onDeletePack } = this.props;
    const begin = (pageSize * (currentPage - 1));
    const end = begin + pageSize;
    const shownItems = items.slice(begin, end);

    return shownItems.map((item) => {
      const { openFunc, installModal } = this._installModal(item);
      let downloadRef;
      const downloadModal = (
        <ContentPackDownloadControl ref={(node) => { downloadRef = node; }}
                                    contentPackId={item.id}
                                    revision={item.rev} />
      );

      const metadata = contentPackMetadata[item.id] || {};
      const installed = Object.keys(metadata).find((rev) => metadata[rev].installation_count > 0);
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
                <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.show(item.id)}>
                  <MenuItem>Show</MenuItem>
                </LinkContainer>
                <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.edit(encodeURIComponent(item.id), encodeURIComponent(item.rev))}>
                  <MenuItem>Create New Version</MenuItem>
                </LinkContainer>
                <MenuItem onSelect={() => { downloadRef.open(); }}>Download</MenuItem>
                <MenuItem divider />
                <MenuItem onSelect={() => { onDeletePack(item.id); }}>Delete All Versions</MenuItem>
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

  render() {
    const { filteredContentPacks, pageSize, currentPage } = this.state;
    const { contentPacks } = this.props;
    const numberPages = Math.ceil(filteredContentPacks.length / pageSize);
    const pagination = (
      <Pagination bsSize="small"
                  bsStyle={`pagination ${ContentPacksListStyle.pager}`}
                  items={numberPages}
                  maxButtons={this.MAX_PAGE_BUTTONS}
                  activePage={currentPage}
                  onSelect={this._onChangePage}
                  prev
                  next
                  first
                  last />
    );
    const pageSizeSelector = (
      <span>Show:&nbsp;
        <select onChange={this._itemsShownChange} value={pageSize}>
          <option>10</option>
          <option>25</option>
          <option>50</option>
          <option>100</option>
        </select>
      </span>
    );

    const noContentMessage = contentPacks.length <= 0
      ? 'No content packs found. Please create or upload one'
      : 'No matching content packs found';
    const content = filteredContentPacks.length <= 0
      ? (<div>{noContentMessage}</div>)
      : (
        <ControlledTableList>
          <ControlledTableList.Header />
          {this._formatItems(filteredContentPacks)}
        </ControlledTableList>
      );

    return (
      <div>
        <Row className="row-sm">
          <Col md={5}>
            <TypeAheadDataFilter id="content-packs-filter"
                                 label="Filter"
                                 data={contentPacks}
                                 displayKey="name"
                                 onDataFiltered={this._filterContentPacks}
                                 searchInKeys={['name', 'summary']}
                                 filterSuggestions={[]} />
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
