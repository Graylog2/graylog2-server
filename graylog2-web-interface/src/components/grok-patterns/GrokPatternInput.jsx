import PropTypes from 'prop-types';
import React from 'react';

import { Input } from 'components/bootstrap';
import { Row, Col, Button, ListGroup, ListGroupItem } from 'react-bootstrap';

import GrokPatternInputStyle from './GrokPatternInput.css';

class GrokPatternInput extends React.Component {
  static propTypes = {
    pattern: PropTypes.string,
    patterns: PropTypes.array,
    onPatternChange: PropTypes.func,
    className: PropTypes.string,
  };

  static defaultProps = {
    pattern: '',
    patterns: [],
    onPatternChange: () => {},
    className: '',
  };

  constructor(props) {
    super(props);
    this.state = {
      patternFilter: '',
      activeListItem: -1,
    };
  }

  shownListItems = [];

  _onPatternChange = (e) => {
    this.props.onPatternChange(e.target.value);
  };

  _onPatternFilterChange = (e) => {
    this.setState({ patternFilter: e.target.value, activeListItem: -1 });
  };

  _onPatternFilterKeyDown = (e) => {
    const ARROW_DOWN = 40;
    const ARROW_UP = 38;
    const ENTER = 13;
    const listItem = this.shownListItems[this.state.activeListItem];

    let activeListItem = 0;
    const firstElement = document.getElementById('list-item-0');
    let domElement;
    let list;
    switch (e.keyCode) {
      case ARROW_DOWN:
        activeListItem = this.state.activeListItem + 1;
        if (activeListItem >= this.shownListItems.length) {
          return;
        }
        domElement = document.getElementById(`list-item-${activeListItem}`);
        list = domElement.parentElement;
        list.scrollTop = domElement.offsetTop - firstElement.offsetTop;
        this.setState({ activeListItem: activeListItem });
        e.preventDefault();
        break;
      case ARROW_UP:
        activeListItem = this.state.activeListItem - 1;
        if (activeListItem < 0) {
          return;
        }
        domElement = document.getElementById(`list-item-${activeListItem}`);
        list = domElement.parentElement;
        list.scrollTop = domElement.offsetTop - firstElement.offsetTop;
        this.setState({ activeListItem: activeListItem });
        e.preventDefault();
        break;
      case ENTER:
        if (listItem) {
          this._addToPattern(listItem);
        }
        e.preventDefault();
        break;
      default:
        break;
    }
  };

  _addToPattern = (name) => {
    const pattern = this.props.pattern || '';
    const index = this.patternInput.getInputDOMNode().selectionStart || pattern.length;
    const newPattern = `${pattern.slice(0, index)}%{${name}}${pattern.slice(index)}`;
    this.props.onPatternChange(newPattern);
  };

  render() {
    const regExp = RegExp(this.state.patternFilter, 'i');
    this.shownListItems = [];
    const patternsToDisplay = this.props.patterns.filter(pattern => regExp.test(pattern.name))
      .map((pattern, index) => {
        const active = index === this.state.activeListItem;
        this.shownListItems.push(pattern.name);
        return (
          <ListGroupItem id={`list-item-${index}`}
                         header={pattern.name}
                         bsStyle={active ? 'info' : undefined}
                         onKeyDown={this._onPatternFilterKeyDown}
                         key={pattern.name}>
            <span className={GrokPatternInputStyle.patternDisplay}>{pattern.pattern}</span>
            <span className={GrokPatternInputStyle.addButton}>
              <Button bsSize="xsmall" bsStyle="primary" onClick={() => { this._addToPattern(pattern.name); }}>
                Add
              </Button>
            </span>
          </ListGroupItem>);
      });
    return (
      <Row bsClass={this.props.className}>
        <Col sm={8}>
          <Input ref={(node) => { this.patternInput = node; }}
                 type="textarea"
                 id="pattern-input"
                 label="Pattern"
                 help="The pattern which will match the log line e.g: '%{IP:client}' or '.*?'"
                 rows={9}
                 onChange={this._onPatternChange}
                 value={this.props.pattern}
                 required />
        </Col>
        <Col sm={4}>
          <Input type="text"
                 id="pattern-selector"
                 label="Filter pattern"
                 onChange={this._onPatternFilterChange}
                 autoComplete="off"
                 formGroupClassName={GrokPatternInputStyle.filterFormGroup}
                 onKeyDown={this._onPatternFilterKeyDown}
                 value={this.state.patternFilter} />
          <ListGroup bsClass={GrokPatternInputStyle.resultList}>{patternsToDisplay}</ListGroup>
        </Col>
      </Row>
    );
  }
}

export default GrokPatternInput;
