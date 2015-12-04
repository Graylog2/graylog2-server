import React from 'react';
import ReactDOM from 'react-dom';

const GridsterWidget = React.createClass({
  propTypes: {
    position: React.PropTypes.object.isRequired,
    grid: React.PropTypes.object.isRequired,
    children: React.PropTypes.node.isRequired,
  },
  componentDidMount() {
    const position = this.props.position;
    const $elem = $(ReactDOM.findDOMNode(this.refs.widget));
    this.props.grid.add_widget($elem, position.width, position.height, position.col, position.row);
  },
  componentWillUnmount() {
    const widgetElem = ReactDOM.findDOMNode(this.refs.widget);
    this.props.grid.remove_widget($(widgetElem));
  },
  render() {
    return (
      <li ref="widget">
        {this.props.children}
      </li>
    );
  },
});

export default GridsterWidget;
