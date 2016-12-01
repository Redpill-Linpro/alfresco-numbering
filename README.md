Alfresco Numbering
=============================================

This module is sponsored by:
* Redpill Linpro AB - http://www.redpill-linpro.com
* Alingsås Kommun - http://www.alingsas.se

Description
-----------
This project contains features for generating unique sequences that can be used to number objects in Alfresco

Structure
------------

The project consists of a repository module and a share module packaged as jar files.

Building & Installation
------------
The build produces several jar files. Attach them to your own maven project using dependencies or put them under tomcat/shared/lib.

Repository dependency:
```xml
<dependency>
  <groupId>org.redpill-linpro.alfresco.numbering</groupId>
  <artifactId>alfresco-numbering-repo</artifactId>
  <version>1.0.1</version>
</dependency>
```

Maven repository:
```xml
<repository>
  <id>redpill-public</id>
  <url>http://maven.redpill-linpro.com/nexus/content/groups/public</url>
</repository>
```

The jar files are also downloadable from: https://maven.redpill-linpro.com/nexus/index.html#nexus-search;quick~alfresco-numbering

Usage
-----

Refer to test-component-context.xml and NumberingComponentIntegrationTest.java for examples on how to use the component. 


License
-------

This application is licensed under the LGPLv3 License. See the [LICENSE file](LICENSE) for details.

Authors
-------

Marcus Svartmark - Redpill Linpro AB

