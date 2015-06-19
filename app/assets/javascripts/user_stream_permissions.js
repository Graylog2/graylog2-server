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
