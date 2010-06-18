function Flotomatic(placeholder, data, options) {
    this.placeholder  = '#' + placeholder;
    this.tooltip      = '#flot_tooltip';
    this.overview     = '#flot_overview';
    this.choices      = '#flot_choices';
    this.data         = data;
    this.options      = options;
    this.plot         = null;
    this.overviewPlot = null;
}

Flotomatic.prototype = {
    createLink: function() {
        var placeholder = jQuery(this.placeholder);

        placeholder.bind("plotclick", function(event, pos, item) {
            var series = item.series,
            dataIndex = item.dataIndex;

            window.open(series.data[dataIndex][3]);
        });
    },

    createTooltip: function() {
        var placeholder = jQuery(this.placeholder),
        tooltip         = jQuery(this.tooltip),
        previousPoint   = null;


        function showTooltip(x, y, contents) {
            jQuery('<div id="flot_tooltip" class="flotomatic_tooltip">' + contents + '</div>').css(
            {
                top: y + 5,
                left: x + 5
            }).appendTo("body").fadeIn(200);
        }

        function tooltipFormatter(item) {
            var date 	 = new Date(item.datapoint[0]),
            label    = item.series.label,
            series = item.series,
            dataIndex = item.dataIndex,
            content = "";

            if (series.data[dataIndex][2] == null){
                content = label + ": " + item.datapoint[1] + " on " + (date.getMonth() + 1) + "/" + date.getDate() + "</a>";
            }
            else {
                content = series.data[dataIndex][2];
            }


            return content;
        }

        placeholder.bind("plothover", this.tooltip, function(event, pos, item) {
            var tooltip = jQuery(event.data);

            if (item) {
                if (previousPoint != item.datapoint) {
                    previousPoint = item.datapoint;

                    tooltip.remove();
                    var x = item.datapoint[0],//.toFixed(2),
                    y = item.datapoint[1]

                    showTooltip(item.pageX, item.pageY, tooltipFormatter(item));
                }
            }
            else {
                tooltip.remove();
                previousPoint = null;
            }
        });
    },

    draw: function(placeholder, data, initialOptions, ranges, dynamic, zoom) {
        var options = initialOptions;

        if (zoom)
            options = jQuery.extend(true, {}, options, {
                selection: {
                    mode: "x"
                },
                xaxis: {
                    min: ranges.xaxis.from,
                    max: ranges.xaxis.to
                }
            });

        return jQuery.plot(placeholder, data, options);
    },

    graph: function(overview, dynamic) {
        var placeholder = jQuery(this.placeholder);

        this.plot = this.draw(placeholder, this.data, this.options);
    },

    graphDynamic: function() {
        var placeholder = jQuery(this.placeholder),
        choices     = jQuery(this.choices),
        options     = this.options,
        data        = this.data,
        i        	= 0;

        jQuery.each(data, function(key, val) {
            if (val.color == null) {
                val.color = i;
            }
            ++i;
        });

        jQuery.each(data, function(key, val) {
            choices.append(choiceFormatter(key, val));
        });

        choices.find("input").click(graphChoices);

        function graphChoices() {
            var set = [];

            choices.find("input:checked").each(function () {
                var key = jQuery(this).attr("name");

                if (key && data[key])
                    set.push(data[key]);
            });

            if (set.length > 0)
                this.plot = jQuery.plot(placeholder, set, options);
        }

        function choiceFormatter(key, val) {
            return '<input type="checkbox" name="' + key + '" checked="checked" > <span class="flot_choice_label">' + val.label + '</span></input> ';
        }

        graphChoices();
    },

    graphOverview: function() {
        var overview        = jQuery(this.overview),
        placeholder 	= jQuery(this.placeholder),
        plot            = this.plot;

        this.overviewPlot = jQuery.plot(overview, this.data, {
            legend: false,
            shadowSize: 0,
            xaxis: {
                ticks: [],
                mode: "time"
            },
            yaxis: {
                ticks: []
            },
            selection: {
                mode: "x"
            }
        });

        placeholder.bind("plotselected", {
            that:this
        }, function (event, ranges) {
            var that   		= event.data.that,
            placeholder = jQuery(that.placeholder);

            that.plot = that.draw(placeholder, that.data, that.options, ranges, false, true);
            that.overviewPlot.setSelection(ranges, true);
        });

        overview.bind("plotselected", {
            that:this
        }, function (event, ranges) {
            event.data.that.plot.setSelection(ranges);
        });
    }
}
