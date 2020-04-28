pipeline
{
   agent
   {
     docker
     {
       image 'torch/jenkins-worker:latest'
       args '-u jenkins:docker -v /home/jenkins/.m2:/home/jenkins/.m2 -v /home/jenkins/.cache:/home/jenkins/.cache -v /var/run/docker.sock:/var/run/docker.sock -v /home/jenkins/.ssh:/home/jenkins/.ssh:ro'
     }
   }

   options
   {
      ansiColor('xterm')
      buildDiscarder logRotator(artifactDaysToKeepStr: '30', artifactNumToKeepStr: '100', daysToKeepStr: '30', numToKeepStr: '100')
      timestamps()
   }

   tools
   {
     maven 'Maven'
   }

   environment
   {
       GITHUB_CREDS = credentials('ea5e9782-80e6-4e2b-a6ef-d19a63f4799b')
       MAVEN_OPTS = '-Djansi.force=true'
       REPO = "${scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]}"
   }

   stages
   {
      stage('Linter')
      {
        when
        {
          expression
          {
            !currentBuild.buildCauses.toString().contains("Branch indexing")
          }
        }
        steps
        {
          echo "Installing node and yarn..."
          sh 'mvn -f graylog2-server/pom.xml clean frontend:install-node-and-yarn frontend:yarn'

          echo "Running linter..."
          sh '''
            set -e

            BRANCH_POINT=`git merge-base refs/remotes/origin/${CHANGE_TARGET} ${GIT_HEAD_SHA1}`
            git diff --name-only --diff-filter=ACMR ${BRANCH_POINT}..${GIT_HEAD_SHA1} | grep -E '^graylog2-web-interface/(.*).js(x)?$' | grep -v flow-typed | sed s,^graylog2-web-interface/,, > /tmp/changed-files

            WEBFILES=`cat /tmp/changed-files`
            CHECKSTYLE_FILE=$HOME/workspace/graylog-pr-linter-check/checkstyle-result-${GIT_HEAD_SHA1}.xml

            rm -f ${CHECKSTYLE_FILE}

            echo "Changing these files for linter hints:"
            echo $WEBFILES

            if [ ! -z "$WEBFILES" ]; then
              cd graylog2-web-interface
              echo $WEBFILES | xargs node/node node_modules/.bin/eslint -c packages/graylog-web-plugin/.eslintrc -f checkstyle -o ${CHECKSTYLE_FILE}
            fi

            exit 0
          '''
        }
      }
   }
   post
   {
     always
     {
         junit testResults: '**/target/surefire-reports/TEST-*.xml'
         recordIssues enabledForFailure: true, tools: [mavenConsole(), java(), javaDoc()]
         recordIssues enabledForFailure: true, tool: checkStyle(pattern: '**/checkstyle-result-${GIT_HEAD_SHA1}.xml')
         recordIssues enabledForFailure: true, tool: spotBugs()
         recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
     }
     cleanup
     {
       cleanWs()
     }
   }
}
