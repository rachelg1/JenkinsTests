import hudson.model.*
import hudson.security.*
import groovy.json.JsonSlurper
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval


String projects_file = readFileFromWorkspace("src/projects.json")

def InputJSON = new JsonSlurper().parseText(projects_file)

InputJSON.projects.each {

        createPipeline(it)
    return true
}

def createPipeline(service) {

    def newJobName = "${service.service_name}_test"

    pipelineJob(newJobName) {

        description("new job haha")

        definition {
            cps {
                script(readFileFromWorkspace('src/main_pipeline.groovy'))

            }
        }

        parameters {
            stringParam("wait_for_scoring", "1", "wait time before scoring path")
        }

    }
}
