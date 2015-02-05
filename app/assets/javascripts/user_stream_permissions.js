/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */

$(document).ready(function() {
    $("#streampermissions")
        .chosen({search_contains:true, width:"250px", inherit_select_classes:true})
        .change(function(event, params) {
            var editSelect = $("#streameditpermissions");
            editSelect.find("option[value=" + params.deselected +"]").attr('selected', false);
            editSelect.chosen().trigger("chosen:updated");
        });

    $("#streameditpermissions")
        .chosen({search_contains:true, width:"250px", inherit_select_classes:true})
        .change(function(event, params){
            if (params.selected) {
                var readSelect = $("#streampermissions");
                readSelect.find("option[value=" + params.selected +"]").attr('selected', true);
                readSelect.chosen().trigger("chosen:updated");
            }
        });

    $("#dashboardpermissions")
        .chosen({search_contains:true, width:"250px", inherit_select_classes:true})
        .change(function(event, params) {
            var editSelect = $("#dashboardeditpermissions");
            editSelect.find("option[value=" + params.deselected +"]").attr('selected', false);
            editSelect.chosen().trigger("chosen:updated");
        });

    $("#dashboardeditpermissions")
        .chosen({search_contains:true, width:"250px", inherit_select_classes:true})
        .change(function(event, params){
            if (params.selected) {
                var readSelect = $("#dashboardpermissions");
                readSelect.find("option[value=" + params.selected +"]").attr('selected', true);
                readSelect.chosen().trigger("chosen:updated");
            }
        });

});
