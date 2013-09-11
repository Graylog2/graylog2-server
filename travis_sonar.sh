env
echo "USER:"
echo $SONAR_MYSQL_USER

mvn sonar:sonar -Dsonar.host.url="http://sonar.torch.sh" \
                -Dsonar.jdbc.url="jdbc:mysql://sonar.torch.sh:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true" \
                -Dsonar.jdbc.username="$SONAR_MYSQL_USER" \
                -Dsonar.jdbc.password="$SONAR_MYSQL_PASS"
