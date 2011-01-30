require 'machinist/active_record'
require 'sham'

Sham.sn { rand(500000) }

Hostgroup.blueprint do
 name { "group-#{sn}" }
end

Stream.blueprint do
  title { "stream-#{sn}" }
end

FilteredTerm.blueprint do
  term { "foo" }
end
