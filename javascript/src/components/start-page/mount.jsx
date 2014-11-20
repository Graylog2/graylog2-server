'use strict';

var React = require('react/addons');

var CardList = require('./CardList');

var cardList = document.getElementById('react-card-list');
if (cardList) {
    React.render(<CardList />, cardList);
}
