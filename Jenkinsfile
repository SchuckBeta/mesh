// The GIT repository for this pipeline lib is defined in the global Jenkins setting
@Library('jenkins-pipeline-library')
import com.gentics.*

// Make the helpers aware of this jobs environment
JobContext.set(this)

properties([disableConcurrentBuilds(),[$class: 'ParametersDefinitionProperty', parameterDefinitions: [
[$class: 'BooleanParameterDefinition', name: 'runTests', defaultValue: true],
[$class: 'BooleanParameterDefinition', name: 'runPerformanceTests', defaultValue: true],
[$class: 'BooleanParameterDefinition', name: 'runDocker', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'runIntegrationTests', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'runReleaseBuild', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'runDeploy', defaultValue: false]
]],
 pipelineTriggers([[$class: 'GitHubPushTrigger'], pollSCM('H/5 * * * *')])
])

final def sshAgent             = "601b6ce9-37f7-439a-ac0b-8e368947d98d"
final def dockerHost           = "tcp://gemini.office:2375"
final def gitCommitTag         = '[Jenkins | ' + env.JOB_BASE_NAME + ']';


node('dockerRoot') {

	def mvnHome = tool 'M3'
	stage("Checkout") {
		sh "rm -rf *"
		sh "rm -rf .git"
		checkout scm
	}
	def branchName = GitHelper.fetchCurrentBranchName()

	stage("Set Version") {
		if (Boolean.valueOf(runReleaseBuild)) {
			version = MavenHelper.getVersion()
			if (version) {
				echo "Building version " + version
				version = MavenHelper.transformSnapshotToReleaseVersion(version)
				MavenHelper.setVersion(version)
			}
			//TODO only add pom.xml files
			sh 'git add .'
			sh "git commit -m 'Raise version'"
			GitHelper.addTag(version, 'Release of version ' + version)
		}
	}

	stage("Test") {
		if (Boolean.valueOf(runTests)) {
			sshagent([sshAgent]) {
				try {
					sh "${mvnHome}/bin/mvn -B -U -e -P inclusions -pl '!demo,!doc,!server,!performance-tests' clean test"
				} finally {
					step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
				}
			}
		} else {
			echo "Tests skipped.."
		}
	}

	stage("Release Build") {
		if (Boolean.valueOf(runReleaseBuild)) {
			sshagent([sshAgent]) {
				sh "${mvnHome}/bin/mvn -B -DskipTests clean package"
			}
		} else {
			echo "Release build skipped.."
		}
	}

	stage("Docker Build") {
		if (Boolean.valueOf(runDocker)) {
			withEnv(["DOCKER_HOST=" + dockerHost ]) {
				sh "rm demo/target/*sources.jar"
				sh "rm server/target/*sources.jar"
				sh "captain build"
			}
		} else {
			echo "Docker build skipped.."
		}
	}

	stage("Performance Tests") {
		if (Boolean.valueOf(runPerformanceTests)) {
			node('cetus') {
				sh "rm -rf *"
				sh "rm -rf .git"
				checkout scm
				//checkout([$class: 'GitSCM', branches: [[name: '*/' + env.BRANCH_NAME]], extensions: [[$class: 'CleanCheckout'],[$class: 'LocalBranch', localBranch: env.BRANCH_NAME]]])
				try {
					sh "${mvnHome}/bin/mvn -B -U clean package -pl '!doc,!demo,!verticles,!server' -Dskip.unit.tests=true -Dskip.performance.tests=false"
				} finally {
					//step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
					step([$class: 'JUnitResultArchiver', testResults: '**/target/*.performance.xml'])
				}
			}
		} else {
			echo "Performance tests skipped.."
		}
	}

	stage("Integration Tests") {
		if (Boolean.valueOf(runIntegrationTests)) {
			withEnv(["DOCKER_HOST=" + dockerHost, "MESH_VERSION=" + version]) {
				sh "integration-tests/test.sh"
			}
		} else {
			echo "Performance tests skipped.."
		}
	}

	stage("Deploy") {
		if (Boolean.valueOf(runDeploy)) {
			if (Boolean.valueOf(runDocker)) {
				withEnv(["DOCKER_HOST=" + dockerHost]) {
					withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'dockerhub_login', passwordVariable: 'DOCKER_HUB_PASSWORD', usernameVariable: 'DOCKER_HUB_USERNAME']]) {
						sh 'docker login -u $DOCKER_HUB_USERNAME -p $DOCKER_HUB_PASSWORD -e entwicklung@genitcs.com'
						sh "captain push"
					}
				}
			}
			sshagent([sshAgent]) {
				sh "${mvnHome}/bin/mvn -U -B -DskipTests clean deploy"
			}
		} else {
			echo "Deploy skipped.."
		}
	}

	stage("Git push") {
		if (Boolean.valueOf(runReleaseBuild)) {

			sshagent([sshAgent]) {
				def snapshotVersion = MavenHelper.getNextSnapShotVersion(version)
				MavenHelper.setVersion(snapshotVersion)
				GitHelper.addCommit('.', gitCommitTag + ' Prepare for the next development iteration (' + snapshotVersion + ')')
				GitHelper.pushBranch(branchName)
				GitHelper.pushTag(version)
			}
		} else {
			echo "Push skipped.."
		}
	}
}
