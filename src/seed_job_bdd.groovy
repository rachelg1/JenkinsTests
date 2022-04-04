import hudson.model.*
import groovy.io.FileType
import hudson.security.*
import groovy.json.JsonSlurper


//File projectsFile = new File(build.getEnvVars()["WORKSPACE"] + "/service_dependencies.json")
String projects_file = readFileFromWorkspace("projects.json")

def InputJSON = new JsonSlurper().parseText(projects_file);

InputJSON.projects.each {
    if (it.group != null && it.service_name != null) {
        //println("project: " + it.group);
        //def newJobName = "BDD_Resilience_" + it.service_name + "_Critical_Dependencies"
        createPipeline(it)
    }
    return true
}
//finally - add to view
addToView()

def addToView(){
    listView('BDD_RESILIENCE') {
        description('All feature files bdd resilience jobs ')
        filterBuildQueue()
        filterExecutors()
        jobs {
            regex('.*bdd_resilience.*')
        }
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
}

def createPipeline(service) {

    def newJobName = "${service.service_name}_bdd_resilience"

    pipelineJob(newJobName) {
        description("BDD test.groovy for ${service.service_name} based on feature file")

        definition {
            cps {
                script(readFileFromWorkspace('jenkins/creator/main_pipeline.groovy'))

            }
        }

        parameters {
            wHideParameterDefinition{
                name("SERVICE_NAME")
                description("service name")
                defaultValue(service.service_name)
            }
            wHideParameterDefinition{
                name("TEST_TYPE")
                description("Test type,generally bdd or critical")
                defaultValue("BDD")
            }
            //stringParam("SERVICE_NAME",service.service_name,"")
            activeChoiceReactiveParam("scenarios"){
                choiceType("MULTI_SELECT")
                referencedParameter('SERVICE_NAME')
                groovyScript {
                    script(readFileFromWorkspace("jenkins/scripts/get_scenarios_bdd.groovy"))

                }
            }
            choiceParam('STRATEGY', ['a_b', 'a_b_c'], 'Choose test.groovy strategy')
            stringParam("wait_for_scoring","1","wait time before scoring path")
            stringParam("duration","120","duration time for each scenario fault injection")

            booleanParam('graphite', true, 'send graphite events during test.groovy')
            wHideParameterDefinition{
                name("web_layer_metric")
                description("The service name of the Nginx/we layer if exist")
                defaultValue(service.web_layer_metric)
            }

        }
    }
}
