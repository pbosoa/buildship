package org.eclipse.buildship.core.workspace.internal

import org.eclipse.buildship.core.CorePlugin
import org.eclipse.buildship.core.test.fixtures.ProjectSynchronizationSpecification;
import org.eclipse.buildship.core.workspace.GradleClasspathContainer

import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IClasspathContainer
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore

class RefreshingTheGradleClasspathContainer extends ProjectSynchronizationSpecification {

    def "Update the project classpath"() {
        setup:
        File location = importNewSimpleProject('simpleproject')
        IJavaProject project = findJavaProject('simpleproject')
        defineLocalGroovyDependency(new File(location, 'build.gradle'))

        when:
        synchronizeAndWait(project.project)

        then:
        hasLocalGroovyDependencyDefinedInClasspathContainer(project)
    }

    def "Update changes the classpath of all related projects"() {
        setup:
        File location = importNewMultiProject('rootproject', 'subproject')
        IJavaProject rootProject = findJavaProject('rootproject')
        IJavaProject subProject = findJavaProject('subproject')

        defineLocalGroovyDependency(new File(location, 'build.gradle'))
        defineLocalGroovyDependency(new File("$location/subproject", 'build.gradle'))

        when:
        synchronizeAndWait(subProject.project)

        then:
        hasLocalGroovyDependencyDefinedInClasspathContainer(rootProject)
        hasLocalGroovyDependencyDefinedInClasspathContainer(subProject)
    }

    def "Updates multiple project roots at the same time"() {
        setup:
        File firstLocation = importNewSimpleProject('first')
        File secondLocation = importNewSimpleProject('second')
        IJavaProject firstProject = findJavaProject('first')
        IJavaProject secondProject = findJavaProject('second')

        defineLocalGroovyDependency(new File(firstLocation, 'build.gradle'))
        defineLocalGroovyDependency(new File(secondLocation, 'build.gradle'))

        when:
        synchronizeAndWait(firstProject.project, secondProject.project)

        then:
        hasLocalGroovyDependencyDefinedInClasspathContainer(firstProject)
        hasLocalGroovyDependencyDefinedInClasspathContainer(secondProject)
    }

    private def importNewSimpleProject(String projectName) {
        def location = newSimpleGradleProject(projectName)
        importAndWait(location)
        location
    }

    private def importNewMultiProject(String rootName, String subName) {
        def location = newGradleMultiProject(rootName, subName)
        importAndWait(location)
        location
    }

    private def newSimpleGradleProject(String projectName) {
        dir(projectName) {
            file 'build.gradle', 'apply plugin: "java"'
            dir 'src/main/java'
        }
    }

    private def newGradleMultiProject(String rootProjectName, String subProjectName) {
        dir(rootProjectName) {
            file 'build.gradle', 'apply plugin: "java"'
            file 'settings.gradle', "include '$subProjectName'"
            dir 'src/main/java'
            dir(subProjectName) {
                file 'build.gradle', 'apply plugin: "java"'
                dir 'src/main/java'
            }
        }
    }

    private static def findJavaProject(String name) {
        JavaCore.create(CorePlugin.workspaceOperations().findProjectByName(name).get())
    }

    private static def defineLocalGroovyDependency(File buildScript) {
        buildScript << '\ndependencies { compile localGroovy() }'
    }

    private static def hasLocalGroovyDependencyDefinedInClasspathContainer(IJavaProject javaProject) {
        IClasspathContainer rootContainer = JavaCore.getClasspathContainer(GradleClasspathContainer.CONTAINER_PATH, javaProject)
        rootContainer.classpathEntries.find  { it.path.toPortableString().contains('groovy-all') } != null
    }

}
