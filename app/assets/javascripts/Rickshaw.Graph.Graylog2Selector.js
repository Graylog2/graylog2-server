Rickshaw.namespace('Rickshaw.Graph.Graylog2Selector');
Rickshaw.Graph.Graylog2Selector = Rickshaw.Class.create({

    initialize: function(args) {
        var graph = args.graph;
        this.graph = args.graph;
        this.build();
        graph.onUpdate(function() { this.update();  }.bind(this));
    },

    build: function() {
        var graph = this.graph;
        var parent = graph.element.getElementsByTagName('svg')[0];

        var selectingActive = false;
        var position = this.position = {};

        // Create selector box.
        var selectionBox = document.createElement('div');
        selectionBox.setAttribute('class','graph-range-selector');
        graph.element.appendChild(selectionBox);

        parent.oncontextmenu = function(){
            e.preventDefault();
        };

        // Function to reset selection.
        var clearSelection = function() {
            selectionBox.style.transition = 'opacity 0.2s ease-out';
            selectionBox.style.opacity = 0;

            setTimeout(function() {
                selectionBox.style.width = 0;
                selectionBox.style.height = 0;
                selectionBox.style.top = 0;
                selectionBox.style.left = 0;
            }, 200);

            parent.style.pointerEvents = 'auto';
            graph.element.style.cursor = 'auto';
        };

        var selectionDraw = function(startPointX) {
            if (selectingActive === true) {
                parent.style.pointerEvents = 'none';
            }

            graph.element.style.cursor = 'crosshair';
            graph.element.addEventListener('mousemove', function(e) {
                if (selectingActive === true) {
                    position.x = e.offsetX | e.layerX;
                    position.deltaX = Math.round(Math.max(position.x, startPointX) - Math.min(position.x, startPointX));
                    position.minX = Math.min(position.x, startPointX);
                    position.maxX = position.minX + position.deltaX;

                    selectionBox.style.transition = 'none';
                    selectionBox.style.opacity = '1';
                    selectionBox.style.width = position.deltaX + "px";
                    selectionBox.style.height = '100%';
                    selectionBox.style.left = position.minX + "px";
                } else {
                    return false;
                }
            }, false);
        };

        // On click in graph until button is released again. (begin of drag)
        graph.element.addEventListener('mousedown', function(e) {
            e.stopPropagation();
            e.preventDefault();

            if (e.button === 0 | e.button === 1) {
                var startPointX = e.layerX;
                selectionBox.style.left = e.layerX;
                selectingActive = true;
                selectionDraw(startPointX);
            } else{
                return;
            }
        }, true);

        // On mouse button release after click. (end of drag / complete)
        graph.element.addEventListener('mouseup', function(e) {
            selectingActive = false;
            position.xMin = Math.round(graph.x.invert(position.minX));
            position.xMax = Math.round(graph.x.invert(position.maxX));

            var from = $('#universalsearch .absolute .absolute-from-human');
            var to = $('#universalsearch .absolute .absolute-to-human');

            if (!isNumber(position.xMin) || !isNumber(position.xMax)) {
                clearSelection();
                return;
            }

            fromDate = momentHelper.toUserTimeZone((position.xMin)*1000);
            toDate = momentHelper.toUserTimeZone((position.xMax)*1000);

            activateTimerangeChooser("absolute", $('.timerange-selector-container .dropdown-menu a[data-selector-name="absolute"]'));
            from.val(fromDate.format(momentHelper.DATE_FORMAT_TZ));
            to.val(toDate.format(momentHelper.DATE_FORMAT_TZ));

            $(".timerange-selector-container").effect("bounce", { complete: function() {
                // Submit search directly if alt key is pressed.
                if(e.altKey) {
                    $("#universalsearch form").submit();
                }
            }});

            clearSelection();
        }, false);

        // Stop at chart boundaries.
        if (graph.dataDomain()[0] === position.xMin) {
            graph.window.xMin = undefined;
        }
        if (graph.dataDomain()[1] === position.xMax) {
            graph.window.xMax = undefined;
        }

        graph.window.xMin = position.xMin;
        graph.window.xMax = position.xMax;
    },

    update: function() {
        var graph = this.graph,
            position = this.position;
        graph.window.xMin = position.xMin;
        graph.window.xMax = position.xMax;

        if (graph.window.xMin === null) {
            position.xMin = graph.dataDomain()[0];
        }

        if (graph.window.xMax === null) {
            position.xMax = graph.dataDomain()[1];
        }

        position.xMin = graph.window.xMin;
        position.xMax = graph.window.xMax;
    }

});

