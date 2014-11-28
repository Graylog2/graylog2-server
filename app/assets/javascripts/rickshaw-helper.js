// We need to override Rickshaw _frequentInterval detection for bar charts due to this issue:
// https://github.com/shutterstock/rickshaw/issues/461
SmartResolutionBarRenderer = Rickshaw.Class.create(Rickshaw.Graph.Renderer.Bar, {
    defaults: function($super) {

        var defaults = Rickshaw.extend( $super(), {
            gapSize: 0.05,
            unstack: false,
            resolution: 'minute'
        } );

        delete defaults.tension;
        return defaults;
    },

    initialize: function($super, args) {
        args = args || {};
        this.resolution = args.resolution || this.resolution;
        $super(args);
    },

    _frequentInterval: function(data) {
        var resolutionDuration = moment.duration(1, this.resolution);
        return { count: 100, magnitude: resolutionDuration.asSeconds() };
    }
});

rickshawHelper = {
    getRenderer: function (renderer) {
        if (renderer == "bar") {
            return SmartResolutionBarRenderer;
        }
        return renderer;
    },

    processHistogramData: function (data, from, to, resolution) {
        this.normalizeHistogramNumbers(data);
        this._correctDataBoundaries(data, from, to, resolution);
    },

    // Statistical functions may return NaN, Infinity and -Infinity as values
    // We need to normalize those numbers into something that makes sense and Rickshaw can represent
    normalizeHistogramNumbers: function (data) {
        data.filter(function(point) {
            return isNaN(point.y) || point.y === "Infinity" || point.y === "-Infinity";
        }).forEach(function(point) {
            point.y = this._normalizeHistogramNumber(point.y);
        }.bind(this));
    },

    _normalizeHistogramNumber: function (number) {
        switch (number) {
            case "NaN":
                return NaN;
            case "Infinity":
                return 0;
            case "-Infinity":
                return 0;
            default:
                return number;
        }
    },

    // Show the whole search time range on charts, even if no data is available.
    _correctDataBoundaries: function (data, from, to, resolution) {
        var fromMoment;
        if (from == undefined || from == null) {
            fromMoment = moment.utc();
        } else {
            fromMoment = moment.utc(from);
        }
        var toMoment = moment.utc(to);

        var formattedResolution = momentHelper.getFormattedResolution(resolution);
        var fromFormatted = fromMoment.startOf(formattedResolution).unix();
        var toFormatted = toMoment.startOf(formattedResolution).unix();

        // Correct left boundary
        this._correctDataLeftBoundary(data, fromFormatted, resolution);

        if (toFormatted != fromFormatted) {
            this._correctDataRightBoundary(data, toFormatted, resolution);
        }
    },

    // Add two points before the actual data if needed to ensure charts look
    // good while we show the whole time range.
    _correctDataLeftBoundary: function (data, boundary, resolution) {
        var firstDataPoint = data[0];
        if (boundary < firstDataPoint.x) {
            var previousPointInTime = moment.unix(firstDataPoint.x).subtract(1, resolution).unix();
            if (previousPointInTime > boundary) {
                data.unshift({"x": previousPointInTime, "y": 0});
            }

            data.unshift({"x": boundary, "y": 0});
        }
    },

    // Add two points after the actual data if needed to ensure charts look
    // good while we show the whole time range.
    _correctDataRightBoundary: function (data, boundary, resolution) {
        var lastDataPoint = data[data.length-1];
        if (boundary > lastDataPoint.x) {
            var nextPointInTime = moment.unix(lastDataPoint.x).add(1, resolution).unix();
            if (nextPointInTime < boundary) {
                data.push({"x": nextPointInTime, "y": 0});
            }
            data.push({"x": boundary, "y": 0});
        }
    }
};