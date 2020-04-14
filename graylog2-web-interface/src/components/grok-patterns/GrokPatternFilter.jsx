import PropTypes from 'prop-types';
import React from 'react';
import { isEqual } from 'lodash';

import { ListGroup, ListGroupItem, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';

import GrokPatternInputStyle from './GrokPatternInput.css';

class GrokPatternInput extends React.Component {
  static propTypes = {
    addToPattern: PropTypes.func.isRequired,
    patterns: PropTypes.array.isRequired,
  };

  shownListItems = [];

  constructor(props) {
    super(props);

    this.state = {
      patternFilter: '',
      activeListItem: -1,
    };
  }

  shouldComponentUpdate(nextProps, nextState) {
    const { patterns } = this.props;
    const { patternFilter, activeListItem } = this.state;

    if (isEqual(nextProps.patterns, patterns) && patternFilter === nextState.patternFilter && activeListItem === nextState.activeListItem) {
      return false;
    }

    return true;
  }


  _onPatternFilterChange = (e) => {
    this.setState({ patternFilter: e.target.value, activeListItem: -1 });
  };

  _onPatternFilterKeyDown = (e) => {
    const { addToPattern } = this.props;
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
          addToPattern(listItem);
        }
        e.preventDefault();
        break;
      default:
        break;
    }
  };

  render() {
    const { activeListItem, patternFilter } = this.state;
    const { addToPattern, patterns } = this.props;
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
              <Button bsSize="xsmall" bsStyle="primary" onClick={() => { addToPattern(displayedPattern.name); }}>
                Add
              </Button>
            </span>
          </ListGroupItem>
        );
      });
    return (
      <>
        <Input type="text"
               id="pattern-selector"
               label="Filter pattern"
               onChange={this._onPatternFilterChange}
               autoComplete="off"
               formGroupClassName={GrokPatternInputStyle.filterFormGroup}
               onKeyDown={this._onPatternFilterKeyDown}
               value={patternFilter} />
        <ListGroup bsClass={GrokPatternInputStyle.resultList}>{patternsToDisplay}</ListGroup>
      </>
    );
  }
}

export default GrokPatternInput;
