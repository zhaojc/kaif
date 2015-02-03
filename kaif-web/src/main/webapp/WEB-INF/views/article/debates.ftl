<#import "/spring.ftl" as spring />
<#import "../macros/template.ftl" as template>

<#assign headContent>

<title>${article.title} | ${zoneInfo.aliasName} | kaif.io</title>

<#-- TODO description and open graph, twitter card...etc -->

<meta name="description"
      content="${article.title} | ${zoneInfo.aliasName} ${zoneInfo.name} | kaif.io">

<link rel="stylesheet" href="/css/${zoneInfo.theme}.css?${(kaif.deployServerTime)!0}">

</#assign>

<@template.page
config={
'layout':'full'
}
head=headContent
>

<div class="zone ${zoneInfo.theme} pure-g article">
    <div class="pure-u-1 pure-u-md-3-4">
    ${article.title}

        <form class="pure-form" debate-form>
            <input type="hidden" id="zoneInput" value="${zoneInfo.zone}">
            <input type="hidden" id="articleInput" value="${article.articleId}">
            <textarea id="contentInput" maxlength="4096" rows="3"></textarea>
            <button type="submit" class="pure-button pure-button-primary">留言</button>
        </form>
        <#list debates as debate>
        ${debate.content}
        </#list>
    </div>
</div>
</@template.page>
