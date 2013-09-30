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

# move development.conf out of the conf folder so it's not included in the package
mv conf/development.conf /tmp/gl2build-development.conf

# .tar.gz
play universal:package-zip-tarball

# move development.conf back
mv /tmp/gl2build-development.conf conf/development.conf

date
echo "Your package(s) are ready at target/universal"
