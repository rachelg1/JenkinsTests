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
                "node() {

    stage(\'critical_dependencies\') {
        Integer A_END = System.currentTimeMillis()/1000
        def A_START = A_END - (3 * 60)
        println(A_END)
        println(A_START)
        }
}"
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
