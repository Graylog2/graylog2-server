
function show_request_params()
{
	$('params_link').hide();
	$('request_params').show();
}
function show_view(page_id){
	['show_sample_summary', 'show_sample_sql', 'show_sample_detail'].each(Element.hide);  
	$(page_id).show();
}
function toggle_row_class(theLink)
{
	var image = theLink.firstChild;
	var visible = toggle_row_class_for_image(image);
	image.src = visible ? EXPANDED_IMAGE : COLLAPSED_IMAGE;
}
function toggle_row_class_for_image(image)
{
	var clazz = image.getAttribute('class_for_children');
	var elements = $('trace_detail_table').select('tr.' + clazz);
	if (elements.length == 0) return;
	var visible = !elements[0].visible();
	show_or_hide_elements(elements, visible);
	return visible;
}
function show_or_hide_class_elements(clazz, visible)
{
	var elements = $('trace_detail_table').select('tr.' + clazz);
	show_or_hide_elements(elements, visible);
}
function show_or_hide_elements(elements, visible)
{
	elements.each(visible ? Element.show : Element.hide);
}

function mouse_over_row(element)
{
	clazz = element.getAttribute('child_row_class')
	element.style.cssText = "background-color:lightyellow";
	return;
	var style_element = get_cleared_highlight_styles();
	style_element.appendChild(document.createTextNode('.' + clazz + ' { opacity: .7; }'));
}

var g_style_element;
function get_cleared_highlight_styles()
{
	if (!g_style_element)
	{
		g_style_element = document.createElement('style');
		g_style_element.setAttribute('id', 'highlight_styles');
		document.getElementsByTagName('head')[0].appendChild(g_style_element);
	}
	else if (g_style_element.lastChild) {
		g_style_element.removeChild(g_style_element.lastChild);
	}
	return g_style_element;
}

function mouse_out_row(element)
{
	element.style.cssText = "";
	return;
	clazz = element.getAttribute('child_row_class')
	get_cleared_highlight_styles();
}
function get_parent_segments()
{
	return $('trace_detail_table').select('img.parent_segment_image');
}

function expand_all_segments()
{
	var parent_segments = get_parent_segments();
	for (var i = 0; i < parent_segments.length; i++)
	{
		parent_segments[i].src = EXPANDED_IMAGE;
		show_or_hide_class_elements(parent_segments[i].getAttribute('class_for_children'), true);
	}
}
function collapse_all_segments()
{
	var parent_segments = get_parent_segments();
	for (var i = 0; i < parent_segments.length; i++)
	{
		parent_segments[i].src = COLLAPSED_IMAGE;
		show_or_hide_class_elements(parent_segments[i].getAttribute('class_for_children'), false);
	}
}
function jump_to_metric(metric_name)
{
	var elements = $('trace_detail_table').select('tr.' + metric_name);
	for (var i = 0; i < elements.length; i++) 
	{
		new Effect.Highlight(elements[i]); //, {endcolor : 'aliceblue'});
		//elements[i].setStyle({backgroundColor: 'lightyellow'});
	}
	expand_all_segments();
}
function sql_mouse_over(id) {
	var sql_div = $('sql_statement' + id);
	if (sql_div)
		Tip(sql_div.innerHTML);
}
function sql_mouse_out(id) {
	UnTip();
} 