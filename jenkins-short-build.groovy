pipeline
{
   agent none

   options
   {
      ansiColor('xterm')
      buildDiscarder logRotator(artifactDaysToKeepStr: '30', artifactNumToKeepStr: '10', daysToKeepStr: '30', numToKeepStr: '10')
      withAWS(region:'eu-west-1', credentials:'aws-key-snapshots')
      skipDefaultCheckout(true) //this pipeline could be triggered from multiple repos, so we will manually clone graylog-project-internal below and let the graylog-project-cli tool clone the other repos for us
      timestamps()
   }

   tools
   {
     maven 'Maven'
   }

   environment
   {
       GITHUB_CREDS = credentials('github-access-token')
       MAVEN_OPTS = '-Djansi.force=true'
       GIT_URL = "${scm.getUserRemoteConfigs()[0].getUrl()}"
       REPO = "${GIT_URL.replace("https://github.com/", "").replace(".git", "")}"
       NODE_OPTIONS = "--max-old-space-size=8192"
   }

   stages
   {
     stage('Build')
     {
       agent
       {
         label 'linux'
       }
       when
       {
         not
         {
           buildingTag()
         }
       }
         steps
         {
           echo "Checking out graylog2-server..."
           checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/build-refactoring']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'WipeWorkspace']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'github-access-token2', url: 'https://github.com/Graylog2/graylog2-server.git']]]

           writeFile file: '.npmrc', text: '''registry=https://nexus.ci.torch.sh/repository/graylog-yarn/
always-auth=true'''

           writeFile file: '.yarnrc', text: 'registry "https://nexus.ci.torch.sh/repository/graylog-yarn/"'

           sh 'mvn --settings ~/.m2/settings-nexus-internal.xml --show-version --batch-mode -Dstyle.color=always --fail-fast -Dspotbugs.skip -Dit.es clean deploy'
         }
     }
   }
 }
