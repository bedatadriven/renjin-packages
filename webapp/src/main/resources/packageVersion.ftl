<#-- @ftlvariable name="version" type="org.renjin.ci.packages.VersionViewModel" -->


<#include "base.ftl">

<@scaffolding title="${version.packageName} ${version.version}">
<div class="grid">
    <div class="medium-12 grid-item">
        
        <#macro depLabel dependency>
        <#if dependency.url?? >
        <a href="${dependency.url}" class="btn">${dependency.label}</a>
        <#else>
        <span class="label">${dependency.label}</span>
        </#if>
        </#macro>
        
        <#macro depList list>
            <#list list as dep>
                <@depLabel dependency=dep/>
            </#list>
        </#macro>
        
        
        <h1>${version.packageName} ${version.version}</h1>
        
        <p class="lead">${version.title}</p>
        
        
        <div class="${version.compatibilityAlert.alertStyle}">${version.compatibilityAlert.message}</div>
        
        <p>${version.descriptionText}</p>
        
        <#--
        <table>
            <tbody>
                <tr>
                    <td>Authors</td>
                    <td>${version.authorList}</td>
                </tr>
                <tr>
                    <td>Maintainer:</td>
                    <td>${version.description.maintainer.name}</td>
                </tr>
                <tr>
                    <td>Imports:</td>
                    <td><@depList list=version.imports/></td>
                </tr>
                <tr>
                    <td>Depends:</td>
                    <td><@depList list=version.depends/></td>
                </tr>
                <tr>
                    <td>Suggests:</td>
                    <td><@depList list=version.suggests/></td>
                </tr>
            </tbody>
        </table>
        -->
        <#if version.available>
        
        <h2>Installation</h2>
        
        <h3>Maven</h3>
        <p>This package can be included as a dependency from a Java or Scala project by including 
        the following your project's <code>pom.xml</code> file. 
        <a href="http://docs.renjin.org/en/latest/introduction.html#setting-up-a-java-project-for-renjin">Read more</a> 
        about embedding Renjin in JVM-based projects.</p>
        <pre>${version.pomReference?html}</pre>
        <p><a href="${version.latestBuildUrl}">View build log</a></p>
        
        <h3>Renjin CLI</h3>
        <p>If you're using Renjin from the command line, you load this library by invoking:</p>
        <pre>${version.renjinLibraryCall?html}</pre>
        </#if>

        <#if (version.exampleResults?size > 0) >
        <h2>Test Results</h2>
        
        <p>This package was last tested against Renjin ${version.exampleRun.renjinVersion} on ${version.exampleRun.time?date}.</p>
        
        <ul class="test-results">
            <#list version.exampleResults as test>
            <li>
                <a href="${version.latestTestRunUrl}#${test.name}-example" class="<#if test.passed>btn btn-success<#else>btn btn-danger</#if>">${test.name}</a>
            </li>
            </#list>
        </ul>
        </#if>
    </div>
</div>


</@scaffolding>