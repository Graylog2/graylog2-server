Sham.login { Faker::Internet.user_name }
Sham.password { Faker::Internet.user_name }
Sham.email { Faker::Internet.email }
Sham.title { Faker::Lorem.words(15).join }
Sham.name { Faker::Lorem.words(15).join }
Sham.host { Faker::Internet.domain_name }

Stream.blueprint do
  shortname SecureRandom.hex(6)
  title
end

Streamrule.blueprint do
end

FilteredTerm.blueprint do
end

User.blueprint do
  login
  password 'testing'
  password_confirmation { password }
  email
  role { :admin }
end

Host.blueprint do
 host
 message_count { rand(50000) }
end

AlertedStream.blueprint do
end

MessageCount.blueprint do
end

Setting.blueprint do
end

ServerValue.blueprint do
end
