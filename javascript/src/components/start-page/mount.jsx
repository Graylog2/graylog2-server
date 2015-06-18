'use strict';

var React = require('react');

var CardList = require('./CardList');

var cardList = document.getElementById('react-card-list');
if (cardList) {
    React.render(<CardList />, cardList);
}
