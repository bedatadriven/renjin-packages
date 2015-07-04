<#-- @ftlvariable name="version" type="org.renjin.ci.packages.PackageVersionPage" -->


<#include "base.ftl">

<@scaffolding title="${version.packageName} ${version.version}">
<div class="grid">
    <div class="medium-8 grid-item">
        
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
        
        <#if version.publicationDate??>
        <p class="sec">Released ${version.publicationDate?date} by ${version.authorList}</p>
        </#if>
        
        <div class="${version.compatibilityAlert.alertStyle}">${version.compatibilityAlert.message}</div>

        <#if (version.dependencies?size > 0)>
        <p><strong>Dependencies:</strong>
        <#list version.dependencies as dependency>
            <a href="${dependency.url}" class="${dependency.style}">${dependency.label}</a>
        </#list>
        </p>
        </#if>
        
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

        <#if (version.testResults?size > 0) >
        <h2>Test Results</h2>
        
        <p>This package was last tested against Renjin ${version.latestBuild.renjinVersion}
            <#if version.latestBuild.startDate??> on ${version.latestBuild.startDate?date}</#if>.</p>
        
        <ul class="test-results">
            <#list version.testResults as test>
            <li>
                <a href="${version.latestBuildUrl}#test-${test.name}" class="<#if test.passed>btn btn-success<#else>btn btn-danger</#if>">${test.name}</a>
            </li>
            </#list>
        </ul>
        </#if>
    </div>
    <div class="grid-item medium-4">
        <div class="inset">
        <h3>Release History</h3>
        <ul>
            <#list version.otherVersions as other>
            <li><a href="${other.path}">${other.version}</a></li>
            </#list>
        </ul>
        </div>
    </div>
</div>


</@scaffolding>