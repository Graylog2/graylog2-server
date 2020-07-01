import React from 'react';

import { Col, Pagination, Row } from 'components/graylog';
import StoreProvider from 'injection/StoreProvider';
import { Spinner } from 'components/common';
import { SystemMessagesList } from 'components/systemmessages';

const SystemMessagesStore = StoreProvider.getStore('SystemMessages');

class SystemMessagesComponent extends React.Component {
  state = { currentPage: 1 };

  componentDidMount() {
    this.loadMessages(this.state.currentPage);
    this.interval = setInterval(() => { this.loadMessages(this.state.currentPage); }, 1000);
  }

  componentWillUnmount() {
    clearInterval(this.interval);
  }

  PER_PAGE = 30;

  loadMessages = (page) => {
    SystemMessagesStore.all(page).then((response) => {
      this.setState(response);
    });
  };

  _onSelected = (selectedPage) => {
    this.setState({ currentPage: selectedPage });
    this.loadMessages(selectedPage);
  };

  render() {
    let content;

    if (this.state.total && this.state.messages) {
      const numberPages = Math.ceil(this.state.total / this.PER_PAGE);

      content = (
        <div>
          <SystemMessagesList messages={this.state.messages} />

          <nav style={{ textAlign: 'center' }}>
            <Pagination totalPages={numberPages}
                        currentPage={this.state.currentPage}
                        onChange={this._onSelected} />
          </nav>
        </div>
      );
    } else {
      content = <Spinner />;
    }

    return (
      <Row className="content">
        <Col md={12}>
          <h2>System messages</h2>

          <p className="description">
            System messages are generated by graylog-server nodes on certain events that may be interesting for
            the Graylog administrators. You don't need to actively act upon any message in here because notifications
            will be raised for any events that required action.
          </p>

          {content}
        </Col>
      </Row>
    );
  }
}

export default SystemMessagesComponent;
