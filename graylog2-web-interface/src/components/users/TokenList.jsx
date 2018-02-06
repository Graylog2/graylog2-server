import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';
import Clipboard from 'clipboard';

import { Row, Col, FormControl, ControlLabel, Button, Checkbox } from 'react-bootstrap';
import UserNotification from 'util/UserNotification';
import TableList from 'components/common/TableList';
import TokenListStyle from './TokenList.css';

class TokenList extends React.Component {
  static propTypes = {
    tokens: PropTypes.arrayOf(PropTypes.object),
    delete: PropTypes.func,
    create: PropTypes.func,
  };

  static defaultProps = {
    tokens: [],
    delete: () => {},
    create: () => {},
  };

  constructor(props) {
    super(props);

    this.state = {
      token_name: '',
      hide_tokens: true,
    };

    this._onNewTokeChanged = this._onNewTokeChanged.bind(this);
    this._onShowTokensChanged = this._onShowTokensChanged.bind(this);
    this._createToken = this._createToken.bind(this);
    this.itemActionsFactory = this.itemActionsFactory.bind(this);
  }

  componentDidMount() {
    this.clipboard = new Clipboard('[data-clipboard-button]');
    this.clipboard.on('success', () => {
      UserNotification.success('Copied to clipboard!');
    });
    this.clipboard.on('error', () => {
      UserNotification.error('Coping failed!');
    });
  }

  componentWillUnmount() {
    if (this.clipboard) {
      this.clipboard.destroy();
    }
  }

  _onNewTokeChanged(event) {
    this.setState({ token_name: event.target.value });
  }

  _onShowTokensChanged(event) {
    this.setState({ hide_tokens: event.target.checked });
  }

  _deleteToken(token) {
    return () => {
      this.props.delete(token.token);
    };
  }

  _createToken() {
    this.props.create(this.state.token_name);
    this.setState({ token_name: '' });
  }

  itemActionsFactory(token) {
    const copyButtonProps = {};
    copyButtonProps['data-clipboard-text'] = token.token;

    return (
      <span>
        <Button data-clipboard-button {...copyButtonProps} bsSize="xsmall">
          Copy to clipboard
        </Button>
        <Button bsSize="xsmall"
          bsStyle="primary"
          onClick={this._deleteToken(token)}>
          Delete
        </Button>
      </span>
    );
  }

  render() {
    const createTokenForm = (
      <form>
        <div className="form-group">
          <Row>
            <Col sm={2}>
              <ControlLabel className={TokenListStyle.tokenNewNameLabel}>Token Name</ControlLabel>
            </Col>
            <Col sm={4}>
              <FormControl
                id="create-token-input"
                type="text"
                placeholder="e.g ServiceName"
                value={this.state.token_name}
                onChange={this._onNewTokeChanged} />
            </Col>
            <Col sm={2}>
              <Button id="create-token"
                      disabled={this.state.token_name === ''}
                      onClick={this._createToken}
                      type="submit"
                      bsStyle="primary" >Create Token</Button>
            </Col>
          </Row>
        </div>
        <hr />
      </form>);

    return (
      <span>
        {createTokenForm}
        <TableList filterKeys={['name', 'token']}
          items={Immutable.List(this.props.tokens)}
          idKey="token"
          titleKey="name"
          descriptionKey="token"
          hideDescription={this.state.hide_tokens}
          itemActionsFactory={this.itemActionsFactory} />
        <Checkbox onChange={this._onShowTokensChanged} checked={this.state.hide_tokens}>Hide Tokens</Checkbox>
      </span>
    );
  }
}

export default TokenList;
