module HealthHelper

	def sorting_link(what, sorting, current_order)
		current_order = "desc" if current_order.blank?

		if !sorting.blank? and sorting == what.to_s
			if current_order == "desc"
				new_order = "asc"
			else
				new_order = "desc"
			end
		else
			new_order = "desc"
		end

		link_to image_tag('icons/table_sorting.gif'), "?sort=#{what}&order=#{new_order}"
	end

end
