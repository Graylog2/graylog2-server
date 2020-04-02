import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col, ListGroup, ListGroupItem, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';

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

  shownListItems = [];

  state = {
    patternFilter: '',
    activeListItem: -1,
  };

  _onPatternChange = (e) => {
    const { onPatternChange } = this.props;
    onPatternChange(e.target.value);
  };

  _onPatternFilterChange = (e) => {
    this.setState({ patternFilter: e.target.value, activeListItem: -1 });
  };

  _onPatternFilterKeyDown = (e) => {
    const { activeListItem } = this.state;
    const ARROW_DOWN = 40;
    const ARROW_UP = 38;
    const ENTER = 13;
    const listItem = this.shownListItems[activeListItem];

    let newActiveListItem = 0;
    const firstElement = document.getElementById('list-item-0');
    let domElement;
    let list;
    switch (e.keyCode) {
      case ARROW_DOWN:
        newActiveListItem = activeListItem + 1;
        if (activeListItem >= this.shownListItems.length) {
          return;
        }
        domElement = document.getElementById(`list-item-${newActiveListItem}`);
        list = domElement.parentElement;
        list.scrollTop = domElement.offsetTop - firstElement.offsetTop;
        this.setState({ activeListItem: newActiveListItem });
        e.preventDefault();
        break;
      case ARROW_UP:
        newActiveListItem = activeListItem - 1;
        if (newActiveListItem < 0) {
          return;
        }
        domElement = document.getElementById(`list-item-${newActiveListItem}`);
        list = domElement.parentElement;
        list.scrollTop = domElement.offsetTop - firstElement.offsetTop;
        this.setState({ activeListItem: newActiveListItem });
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
    const { pattern, onPatternChange } = this.props;
    const index = this.patternInput.getInputDOMNode().selectionStart || pattern.length;
    const newPattern = `${pattern.slice(0, index)}%{${name}}${pattern.slice(index)}`;
    onPatternChange(newPattern);
  };

  render() {
    const { activeListItem, patternFilter } = this.state;
    const { className, patterns, pattern } = this.props;
    const regExp = RegExp(patternFilter, 'i');
    this.shownListItems = [];
    const patternsToDisplay = patterns.filter((displayedPattern) => regExp.test(displayedPattern.name))
      .map((displayedPattern, index) => {
        const active = index === activeListItem;
        this.shownListItems.push(displayedPattern.name);
        return (
          <ListGroupItem id={`list-item-${index}`}
                         header={displayedPattern.name}
                         bsStyle={active ? 'info' : undefined}
                         onKeyDown={this._onPatternFilterKeyDown}
                         key={displayedPattern.name}>
            <span className={GrokPatternInputStyle.patternDisplay}>{displayedPattern.pattern}</span>
            <span className={GrokPatternInputStyle.addButton}>
              <Button bsSize="xsmall" bsStyle="primary" onClick={() => { this._addToPattern(displayedPattern.name); }}>
                Add
              </Button>
            </span>
          </ListGroupItem>
        );
      });
    return (
      <Row className={className}>
        <Col sm={8}>
          <Input ref={(node) => { this.patternInput = node; }}
                 type="textarea"
                 id="pattern-input"
                 label="Pattern"
                 help="The pattern which will match the log line e.g: '%{IP:client}' or '.*?'"
                 rows={9}
                 onChange={this._onPatternChange}
                 value={pattern}
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
                 value={patternFilter} />
          <ListGroup bsClass={GrokPatternInputStyle.resultList}>{patternsToDisplay}</ListGroup>
        </Col>
      </Row>
    );
  }
}

export default GrokPatternInput;
