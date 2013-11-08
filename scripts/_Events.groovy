import groovy.xml.MarkupBuilder
import groovy.xml.NamespaceBuilder
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
//import grails.util.Metadata
//import org.mortbay.jetty.webapp.*
//import org.mortbay.jetty.handler.*

compileStyleSheets = { dir ->
  def ant = new AntBuilder()   // create an antbuilder
  if (System.properties['os.name'].toLowerCase().contains('windows')) {
    ant.exec(
            failonerror: "true",
            dir: "${dir}/themes",
            executable: 'cmd') {

      arg(value: "/c")
      arg(value: "compile_all_themes.bat")
    }
  }
  else {
    ant.exec(
            failonerror: "true",
            dir: "${dir}/themes",
            executable: 'bash') {

      arg(value: "-l")
      arg(value: "compile_all_themes.sh")
    }
  }

  //delete cache files
  ant.delete(includeemptydirs:true) {
    fileset(dir:"${dir}") {
      include(name:"**/.sass-cache/**")
    }
  }

  println "finished compiling sass stylesheets"
}


eventRunAppHttpsStart = {
  def baseWebDir = "${basedir}/web-app"

  if (!System.properties.skipSassCompile) {
    println "compiling sass stylesheets - ruby, compass, and sass must be installed"
    println "compiling stylesheets in web-app"
    compileStyleSheets(baseWebDir)

    println "compiling stylesheets in external themes dir"
    compileStyleSheets(basedir)
  }
  
  if(!System.properties.skipCopyAppConfig) {
	  def applicationConfigUiDir = "${baseWebDir}/js/components/admin/applicationConfiguration"
	  println "Use -DskipCopyAppConfig=true to omit this step"
	  copyAppConfigFiles applicationConfigUiDir
	  println "Finished copying app configuration files"
  }
  
}

eventRunAppStart = {
    def baseWebDir = "${basedir}/web-app"

    if (!System.properties.skipSassCompile) {
        println "compiling sass stylesheets - ruby, compass, and sass must be installed"
        println "compiling stylesheets in web-app"
        compileStyleSheets(baseWebDir)

        println "compiling stylesheets in external themes dir"
        compileStyleSheets(basedir)
    }

	if(!System.properties.skipCopyAppConfig) {
		def applicationConfigUiDir = "${baseWebDir}/js/components/admin/applicationConfiguration"
		println "Use -DskipCopyAppConfig=true to omit this step"
		copyAppConfigFiles applicationConfigUiDir
		println "Finished copying app configuration files"
	}
	
}

eventCreateWarStart = { name, stagingDir ->

  def applicationConfigUiDir = "${stagingDir}/js/components/admin/applicationConfiguration"
  copyAppConfigFiles applicationConfigUiDir


  println "copying help for help into war"

    def baseWebDir = "${basedir}/web-app"
    ant.copy(todir: "${baseWebDir}/help") {
      fileset(dir: "${basedir}/docs-internal") {
        include(name: "Instructions_for_configuring_Help_content.pdf")
        include(name: "Keyboard Navigation.pdf")
      }
    }
    ant.copy(todir: "${stagingDir}/help") {
      fileset(dir: "${basedir}/docs-internal") {
        include(name: "Instructions_for_configuring_Help_content.pdf")
        include(name: "Keyboard Navigation.pdf")
      }
    }

  println "finished help for help into war"

  println "making owf-all.jar"

      //jar up all files in WEB-INF/classes and put into WEB-INF/lib/owf-all.jar
      ant.jar(destfile:"${stagingDir}/WEB-INF/lib/owf-all.jar", update:false) {
        fileset(dir:"${stagingDir}/WEB-INF/classes") {
          exclude(name:"**/gsp_*.*")
          exclude(name:"**/*.properties")
          exclude(name:"**/*.xml")
          exclude(name:"**/.gitignore")
        }
      }

      ant.delete(includeemptydirs:true) {
        fileset(dir:"${stagingDir}/WEB-INF/classes") {
          exclude(name:"**/gsp_*.*")
          exclude(name:"**/*.properties")
          exclude(name:"**/*.xml")
        }
      }

  println "finished making owf-all.jar"

  // Copy descriptor template into war stage in such a way that it will
  // always be in the CLASSPATH when deployed
  ant.copy(file: "${basedir}/src/resources/empty_descriptor.html",
           todir: "${stagingDir}/WEB-INF/classes")
}


copyAppConfigFiles = { destinationDir ->

	
	File pluginDir = new File("${basedir}/plugins/").eachDir{
		if(it =~ /aml-commons-appconfig/){
			String appConfigPluginDir = it.getName()
			String sourceDir = "${basedir}/plugins/${appConfigPluginDir}/web-app/js/applicationConfiguration"
			new AntBuilder().copy(todir: destinationDir) {
				fileset(dir: sourceDir)
			}
			return
		}
	}	

}
