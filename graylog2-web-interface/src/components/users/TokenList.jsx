import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col, Form, FormControl, FormGroup, ControlLabel, InputWrapper, Button } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import ClipboardButton from 'components/common/ClipboardButton';
import TokenListStyle from '!style!css!./TokenList.css';

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
    };

    this._onNewTokeChanged = this._onNewTokeChanged.bind(this);
    this._createToken = this._createToken.bind(this);
  }

  _onNewTokeChanged(e) {
    this.setState({ token_name: e.target.value });
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

  _tokenItem(token, idx) {
    return (
      <Form horizontal>
        <Input label={token.name}
               id={`token-${idx}-Id`}
               type="text"
               readonly
               value={token.token}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-6" />
        <div className="form-group">
          <Col smOffset={3} sm={9}>
            <ClipboardButton title="Copy" target={`#token-${idx}-Id`} />
            {' '}
            <Button id={`delete-user-${token.name}`}
                    bsStyle="primary"
                    title="Delete token"
                    onClick={this._deleteToken(token)}>
              Delete
            </Button>
          </Col>
        </div>
      </Form>
    );
  }

  render() {
    const tokenInputs = this.props.tokens.map((token, idx) => this._tokenItem(token, idx));
    return (
      <span>
        <span>{tokenInputs}</span>
        <div className={TokenListStyle.tokenCreateForm}>
          <Form horizontal>
            <Input label="Token Name"
                   id="create-token-input"
                   type="text"
                   placeholder="e.g ServiceName"
                   value={this.state.token_name}
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9"
                   onChange={this._onNewTokeChanged} />
            <div className="form-group">
              <Col smOffset={3} sm={9}>
                <Button id="create-token" onClick={this._createToken} bsStyle="primary" >Create Token</Button>
              </Col>
            </div>
          </Form>
        </div>
      </span>
    );
  }
}

export default TokenList;
