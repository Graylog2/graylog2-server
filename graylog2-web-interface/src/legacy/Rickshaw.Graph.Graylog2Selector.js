import $ from 'jquery';
import {} from 'jquery-ui/ui/effects/effect-bounce';
import Rickshaw from 'rickshaw';
import NumberUtils from 'util/NumberUtils';
import DateTime from 'logic/datetimes/DateTime';

import StoreProvider from 'injection/StoreProvider';
const SearchStore = StoreProvider.getStore('Search');

Rickshaw.namespace('Rickshaw.Graph.Graylog2Selector');

const Graylog2Selector = Rickshaw.Class.create({

  initialize(args) {
    const graph = args.graph;
    this.graph = args.graph;
    this.build();
    graph.onUpdate(() => {
      this.update();
    });
  },

  build() {
    const graph = this.graph;
    const parent = graph.element.getElementsByTagName('svg')[0];

    let selectingActive = false;
    const position = this.position = {};

        // Create selector box.
    const selectionBox = document.createElement('div');
    selectionBox.setAttribute('class', 'graph-range-selector');
    graph.element.appendChild(selectionBox);

    parent.oncontextmenu = function () {
      e.preventDefault();
    };

        // Function to reset selection.
    const clearSelection = function () {
      selectionBox.style.transition = 'opacity 0.2s ease-out';
      selectionBox.style.opacity = 0;

      setTimeout(() => {
        selectionBox.style.width = 0;
        selectionBox.style.height = 0;
        selectionBox.style.top = 0;
        selectionBox.style.left = 0;
      }, 200);

      parent.style.pointerEvents = 'auto';
      graph.element.style.cursor = 'auto';
    };

    const selectionDraw = function (startPointX) {
      if (selectingActive === true) {
        parent.style.pointerEvents = 'none';
      }

      graph.element.style.cursor = 'crosshair';
      graph.element.addEventListener('mousemove', (e) => {
        if (selectingActive === true) {
          position.x = e.offsetX | e.layerX;
          position.deltaX = Math.round(Math.max(position.x, startPointX) - Math.min(position.x, startPointX));
          position.minX = Math.min(position.x, startPointX);
          position.maxX = position.minX + position.deltaX;

          selectionBox.style.transition = 'none';
          selectionBox.style.opacity = '1';
          selectionBox.style.width = `${position.deltaX}px`;
          selectionBox.style.height = '100%';
          selectionBox.style.left = `${position.minX}px`;
        } else {
          return false;
        }
      }, false);
    };

        // On click in graph until button is released again. (begin of drag)
    graph.element.addEventListener('mousedown', (e) => {
      e.stopPropagation();
      e.preventDefault();

      if (e.button === 0 | e.button === 1) {
        const startPointX = e.layerX;
        selectionBox.style.left = e.layerX;
        selectingActive = true;
        selectionDraw(startPointX);
      } else {

      }
    }, true);

        // On mouse button release after click. (end of drag / complete)
    graph.element.addEventListener('mouseup', (e) => {
      selectingActive = false;
      position.xMin = Math.round(graph.x.invert(position.minX));
      position.xMax = Math.round(graph.x.invert(position.maxX));

      const from = $('#universalsearch .absolute .absolute-from-human');
      const to = $('#universalsearch .absolute .absolute-to-human');

      if (!NumberUtils.isNumber(position.xMin) || !NumberUtils.isNumber(position.xMax)) {
        clearSelection();
        return;
      }

      const fromDate = new DateTime((position.xMin) * 1000);
      const toDate = new DateTime((position.xMax) * 1000);

      SearchStore.changeTimeRange('absolute', { from: fromDate.toString(), to: toDate.toString() });

      $('.timerange-selector-container').effect('bounce', {
        complete() {
                    // Submit search directly if alt key is pressed.
          if (e.altKey) {
            submitSearch();
          }
        },
      });

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

  update() {
    let graph = this.graph,
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
  },

});

Rickshaw.Graph.Graylog2Selector = Graylog2Selector;
export default Graylog2Selector;
