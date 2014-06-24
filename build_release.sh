# http://www.playframework.com/documentation/2.2.0/ProductionDist

read -p "Did you bump both app/lib/Version.java and project/Build.scala? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
  echo "Great! Starting the build process."
else
  echo "Please do that."
  exit 1
fi  

# move configs around so we have our standard config packaged
mv -f conf/graylog2-web-interface.conf /tmp/gl2build-tmp.conf
cp misc/graylog2-web-interface.conf.example conf/graylog2-web-interface.conf

# .tar.gz
play universal:package-zip-tarball

# move local development config back
mv /tmp/gl2build-tmp.conf conf/graylog2-web-interface.conf

date
echo "Your package(s) are ready at target/universal"
