namespace :test do
  namespace :travis do
    task :copy_configs do
      Dir.glob('config/*.yml.example') do |from|
        to = 'config/' + File.basename(from, '.example')
        FileUtils.copy(from, to, :verbose => true)
      end
    end
  end
end
