'use strict';

var $ = require('jquery');

var d3 = require('d3');
var dc = require('dc');

var UniversalSearch = require('../search/UniversalSearch');

var othersThreshold = 5;
var othersName = "Others";

var SourceDataTable = {
    renderDataTable(dimension, numberOfSources, onDataFiltered) {
        var dataTableDomNode = $("#dc-sources-result")[0];
        this.dataTable = dc.dataTable(dataTableDomNode);
        this.dataTable
            .dimension(dimension)
            .group((d) => d.percentage > othersThreshold ? "Top Sources" : othersName)
            .size(numberOfSources)
            .columns([
                (d) => "<button class='btn btn-mini btn-link dc-search-button' title='Search for this source'><i class='icon icon-search'></i></button>",
                (d) => "<a href='javascript:undefined' class='dc-filter-link' title='Filter this source'>" + d.name +"</a>",
                (d) => d.percentage.toFixed(2) + "%",
                (d) => d.message_count
            ])
            .sortBy((d) => d.message_count)
            .order(d3.descending)
            .renderlet((table) => {
                table.selectAll("td.dc-table-column._0 button.dc-search-button").on("click", () => {
                    // d3 doesn't pass any data to the onclick event as the buttons do not
                    // have any. Instead, we need to get it from the table element.
                    var parentTdElement = $(d3.event.target).parents("td.dc-table-column._0");
                    var datum = d3.selectAll(parentTdElement).datum();
                    var source = datum.name;
                    UniversalSearch.addSegment(UniversalSearch.createSourceQuery(source), UniversalSearch.orOperator());
                    if (d3.event.altKey) {
                        UniversalSearch.submit();
                    }
                });
            })
            .renderlet((table) => {
                table.selectAll("td.dc-table-column._1 a.dc-filter-link").on("click", () => {
                    var parentTdElement = $(d3.event.target).parents("td.dc-table-column._1");
                    var datum = d3.selectAll(parentTdElement).datum();

                    onDataFiltered(datum.name);
                });
            })
            .renderlet((table) => table.selectAll(".dc-table-group").classed("info", true));
    },
    changeNumberOfSources(numberOfSources) {
        this.dataTable
            .size(numberOfSources)
            .redraw();
    }
};

module.exports = SourceDataTable;