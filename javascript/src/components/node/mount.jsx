'use strict';

var React = require('react/addons');
var JvmHeapUsage = require('./JvmHeapUsage');
var BufferUsage = require('./BufferUsage');
var JournalDetails = require('./JournalDetails');
var JournalState = require('./JournalState');

var heapUsage = document.getElementsByClassName('react-jvm-heap');
if (heapUsage) {
    for (var i = 0; i < heapUsage.length; i++) {
        var elem = heapUsage[i];
        var id = elem.getAttribute('data-node-id');
        React.render(<JvmHeapUsage nodeId={id}/>, elem);
    }
}

var buffers = document.getElementsByClassName('react-buffer-usage');
if (buffers) {
    for (var i = 0; i < buffers.length; i++) {
        var elem = buffers[i];
        var id = elem.getAttribute('data-node-id');
        var title = elem.getAttribute('data-title');
        var bufferType = elem.getAttribute('data-buffer-type');
        React.render(<BufferUsage nodeId={id} title={title} bufferType={bufferType}/>, elem);
    }
}

var journal = document.getElementById('react-journal-info');
if (journal) {
    var id = elem.getAttribute('data-node-id');
    var dir = journal.getAttribute('data-journal-dir');
    var enabled = journal.getAttribute('data-journal-enabled');
    var maxSize = journal.getAttribute('data-journal-max-size');
    var maxAge = journal.getAttribute('data-journal-maxage');
    var flushInterval = journal.getAttribute('data-journal-flush-interval');
    var flushAge = journal.getAttribute('data-journal-flush-age');

    React.render(<JournalDetails nodeId={id} directory={dir} enabled={enabled} maxSize={maxSize} maxAge={maxAge}
                                 flushInterval={flushInterval} flushAge={flushAge}/>, journal);
}

var journalStates = document.getElementsByClassName('react-journal-state');
if (journalStates) {
    for (var i = 0; i < journalStates.length; i++) {
        var elem = journalStates[i];
        var id = elem.getAttribute('data-node-id');
        React.render(<JournalState nodeId={id}/>, elem);
    }
}