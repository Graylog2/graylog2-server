import PropTypes from 'prop-types';
import React from 'react';

import { ListGroup, ListGroupItem, Button } from 'components/graylog';

import PatternListGroupStyle from './GrokPatternInput.css';

const PatternListGroup = ({
  addToPattern,
  className,
  onPatternFilterKeyDown,
  patternFilter,
  patterns,
}) => {
  const shownListItems = [];
  const regExp = RegExp(patternFilter, 'i');

  const patternsToDisplay = patterns.filter((displayedPattern) => regExp.test(displayedPattern.name))
    .map((displayedPattern, index) => {
      shownListItems.push(displayedPattern.name);

      return (
        <ListGroupItem id={`list-item-${index}`}
                       header={displayedPattern.name}
                       onKeyDown={onPatternFilterKeyDown}
                       key={displayedPattern.name}>
          <span className={PatternListGroupStyle.patternDisplay}>{displayedPattern.pattern}</span>
          <span className={PatternListGroupStyle.addButton}>
            <Button bsSize="xsmall"
                    bsStyle="primary">
              Add
            </Button>
          </span>
        </ListGroupItem>
      );
    });

  return (
    <ListGroup bsClass={PatternListGroupStyle.resultList} className={className}>
      {patternsToDisplay}
    </ListGroup>
  );
};

PatternListGroup.propTypes = {
  patterns: PropTypes.any,
  patternFilter: PropTypes.any,
  className: PropTypes.string,
  addToPattern: PropTypes.func,
  onPatternFilterKeyDown: PropTypes.func,
};

PatternListGroup.defaultProps = {
  patterns: [],
  patternFilter: '',
  className: '',
  addToPattern: () => {},
  onPatternFilterKeyDown: () => {},
};

export default PatternListGroup;
