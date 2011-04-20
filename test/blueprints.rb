Sham.login { Faker::Internet.user_name }
Sham.password { Faker::Internet.user_name }
Sham.email { Faker::Internet.email }
Sham.title { Faker::Lorem.words(15) }
Sham.name { Faker::Lorem.words(15) }
Sham.host { Faker::Internet.domain_name }

Message.blueprint do
 message { Faker::Lorem.words(15) }
 facility { rand(15) }
 level { rand(8) }
 host
 created_at { Time.now.to_f }
 deleted { false }
end

Stream.blueprint do
  title
end

Streamrule.blueprint do
end

FilteredTerm.blueprint do
end

User.blueprint do
  login
  password
  password_confirmation { password }
  email
end

Host.blueprint do
 host
 message_count { rand(50000) }
end

Hostgroup.blueprint do
  name
end

HostgroupHost.blueprint do
  hostname { host }
end
