namespace :test do
  namespace :travis do
    desc "Copy config/*.yml.example to config/*.yml"
    task :copy_configs do
      Dir.glob('config/*.yml.example') do |from|
        to = 'config/' + File.basename(from, '.example')
        FileUtils.copy(from, to, :verbose => true)
      end
    end

  end
end
