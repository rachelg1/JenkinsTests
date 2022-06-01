import hudson.model.*
import groovy.io.FileType
import hudson.security.*
import groovy.json.JsonSlurper
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval


//File projectsFile = new File(build.getEnvVars()["WORKSPACE"] + "/service_dependencies.json")
String projects_file = readFileFromWorkspace("src/projects.json")

def InputJSON = new JsonSlurper().parseText(projects_file)

InputJSON.projects.each {
    if (it.group != null && it.service_name != null) {
        //println("project: " + it.group);
        //def newJobName = "BDD_Resilience_" + it.service_name + "_Critical_Dependencies"
        createPipeline(it)
    }
    return true
}

def createPipeline(service) {

    def newJobName = "${service.service_name}_test"

    pipelineJob(newJobName) {

        description("BDD test.groovy for ${service.service_name} based on feature file")

        definition {
            cps {
                script(
                node (){
    def currentStage = "";
    def String[] scenarios_arr
    def failed_scenarios = []
    scenarios_arr = "s1,s2,s3".split(',')
    scenarios_arr.each { entry ->

        //catchError(message: "scenario : $entry - failed", buildResult: 'FAILURE', stageResult: 'FAILURE') {
        stage(entry) {
            try {
                echo "$entry"
                currentStage = entry
                if (entry == "s2")
                    throw new Exception("hello")
            }
            //}
            catch (e) {
                echo 'scenario : ' + entry + ' - failed: ' + e
                failed_scenarios.add(entry)
                // Since we're catching the exception in order to report on it,
                // we need to re-throw it, to ensure that the build is marked as failed
                //throw e
            }
        }

    }

    if (failed_scenarios.size() != 0) {
        echo 'failed_scenarios: ' + "[\"${failed_scenarios.join('", "')}\"]"
        currentBuild.result = 'FAILURE'
    }

}

def runBehaveBDD(scenario, A_START, A_END) {
    sh '''set -x
echo "set up variable for the shell"
    scenario="''' + scenario + '''"
'''

}
                )

            }
        }

        parameters {
            choiceParam('STRATEGY', ['a_b', 'a_b_c'], 'Choose test.groovy strategy')
            stringParam("wait_for_scoring", "1", "wait time before scoring path")
            stringParam("duration", "120", "duration time for each scenario fault injection")

            booleanParam('graphite', true, 'send graphite events during test.groovy')
        }


    }
}
