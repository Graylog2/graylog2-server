import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';

import ClipboardButton from 'components/common/ClipboardButton';
import { Row, Col, FormControl, ControlLabel, Button, Checkbox, ButtonGroup } from 'react-bootstrap';
import TableList from 'components/common/TableList';
import Spinner from 'components/common/Spinner';
import TokenListStyle from './TokenList.css';

class TokenList extends React.Component {
  static propTypes = {
    tokens: PropTypes.arrayOf(PropTypes.object),
    onDelete: PropTypes.func,
    onCreate: PropTypes.func,
    creatingToken: PropTypes.bool,
    deletingToken: PropTypes.string,
  };

  static defaultProps = {
    tokens: [],
    onDelete: () => {},
    onCreate: () => {},
    creatingToken: false,
    deletingToken: undefined,
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

  _onNewTokeChanged(event) {
    this.setState({ token_name: event.target.value });
  }

  _onShowTokensChanged(event) {
    this.setState({ hide_tokens: event.target.checked });
  }

  _deleteToken(token) {
    return () => {
      this.props.onDelete(token.token, token.name);
    };
  }

  _createToken(e) {
    this.props.onCreate(this.state.token_name);
    this.setState({ token_name: '' });
    e.preventDefault();
  }

  itemActionsFactory(token) {
    const deleteButton = this.props.deletingToken === token.token ? <Spinner text="Deleting..." /> : 'Delete';
    return (
      <ButtonGroup>
        <ClipboardButton title={'Copy to clipboard'} text={token.token} bsSize="xsmall" />
        <Button bsSize="xsmall"
                disabled={this.props.deletingToken === token.token}
                bsStyle="primary"
                onClick={this._deleteToken(token)}>
          {deleteButton}
        </Button>
      </ButtonGroup>
    );
  }

  render() {
    const submitButton = (this.props.creatingToken ? <Spinner text="Creating..." /> : 'Create Token');

    const createTokenForm = (
      <form onSubmit={this._createToken}>
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
              disabled={this.state.token_name === '' || this.props.creatingToken}
              type="submit"
              bsStyle="primary" >{submitButton}</Button>
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
          enableBulkActions={false}
          itemActionsFactory={this.itemActionsFactory} />
        <Checkbox id="hide-tokens" onChange={this._onShowTokensChanged} checked={this.state.hide_tokens}>
          Hide Tokens
        </Checkbox>
      </span>
    );
  }
}

export default TokenList;
