# folio-custom-fields

Copyright (C) 2019-2024 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

<!-- ../../okapi/doc/md2toc -l 2 -h 4 README.md -->
* [Introduction](#introduction)
* [Module Deployment](#module-deployment)
* [Issue tracker](#issue-tracker)
* [Code analysis](#code-analysis)

## Introduction

The FOLIO module to store and maintain custom fields. The folio-custom-fields is designed to be a
library that any module can add and use to store custom fields definitions as well as their values.

The custom fields module implements a simple CRUD interface POST/PUT/GET/DELETE on /custom-fields and /custom-fields/$id endpoints. See the ramls/custom-fields.json for precise definitions.

Written in Java, using the raml-module-builder and uses Maven as its build system.

The general design of the custom fields is following

![](images/custom-fields-design.png)

The module has a common interface which can be used by several modules. 
This feature called *Multiple interfaces* and helps OKAPI to dispatch the request to any number of modules with the same interface just use "interfaceType": "multiple"  in the "Provides" section of the ModuleDescriptor.json file
To define a particular module to be called to, declare `X-Okapi-Module-Id` header, detailed information is able via the link to [Okapi documentation](https://github.com/folio-org/okapi/blob/master/doc/guide.md#multiple-interfaces).
The additional information about folio-custom-fields and sample request collection can be found in [Confluence](https://wiki.folio.org/pages/viewpage.action?spaceKey=FOLIJET&title=MODCFIELDS-39+-+Custom+Field+backend+demo)

## Module Deployment

The custom fields functionality can be added to any module as a jar file.
The target module needs to carry out the changes described below. 
Please note that indicated module version and ModuleDescriptor.json file serve informational purpose.  
  1. Add maven dependency for `folio-custom-fields` in project pom file.
  Example:
   ~~~~
   <dependency>
     <groupId>org.folio</groupId>
     <artifactId>folio-custom-fields</artifactId>
     <version>1.4.1</version>
   </dependency>
   ~~~~
  We recommend using the latest released version. The list of released versions is available via the [link](https://github.com/folio-org/folio-custom-fields/releases).
  2. Modify the section `scripts` in schema.json file to include `create_custom_fields_table.sql`, which will created a table for storing module-specific custom fields and additional triggers.
  Example:
  ~~~~
  "scripts" : [
      {
        "run": "after",
        "snippetPath": "create_custom_fields_table.sql",
        "fromModuleVersion": "1.0"
      }
    ]
  ~~~~
  3. Modify the ModuleDescriptor.json file by including the custom-fields interface
  Example:   
  ~~~~
  {
    "id": "custom-fields",
    "version": "1.0",
    "interfaceType" : "multiple",
    "handlers": [
      {
        "methods": ["GET"],
        "pathPattern": "/custom-fields",
        "permissionsRequired": ["custom.fields.collection.get"]
      },
      {
        "methods": ["POST"],
        "pathPattern": "/custom-fields",
        "permissionsRequired": ["custom.fields.item.post"]
      },
      {
        "methods": ["GET"],
        "pathPattern": "/custom-fields/{id}",
        "permissionsRequired": ["custom.fields.item.get"]
      },
      {
        "methods": ["PUT"],
        "pathPattern": "/custom-fields/{id}",
        "permissionsRequired": ["custom.fields.item.put"]
      },
      {
        "methods": ["DELETE"],
        "pathPattern": "/custom-fields/{id}",
        "permissionsRequired": ["custom.fields.item.delete"]
      }
    ]
   }
   ~~~~ 
  The permission name inside of the `permissionsRequired` section can be modified to represent the module purpose.
  See [mod-users](https://github.com/folio-org/mod-users/pull/136/files) as an example of `folio-custom-fields` integration.

## RecordService

The [`RecordService`](src/main/java/org/folio/service/RecordService.java) is responsible for updating entities when
custom fields are updated. This means that whenever a custom field is modified, the `RecordService` ensures that these
changes are reflected in the corresponding entities. Additionally, the `RecordService` provides statistics on how 
many entities are using a particular custom field or option.

Two different implementations of the `RecordService` interface are available:

By default the [`NoOpRecordService`](src/main/java/org/folio/service/NoOpRecordService.java) is used. This is a basic 
implementation of the `RecordService` that does not update any entities and returns statistics with zero counts.

The [`RecordServiceImpl`](src/main/java/org/folio/service/RecordServiceImpl.java) implementation does update 
entities and returns statistics based on the actual usage of custom fields or their options. It can be configured to 
support multiple tables for each `entityType`.

To use the `RecordServiceImpl`, follow these steps:

1. Implement the `RecordServiceFactory` interface to return a new instance of the 
   `RecordServiceImpl` implementation with the specified entity types and tables. The map takes 
   entity types as key and the corresponding table name as value.

    ```java
    package org.folio.services.record;
    
    import io.vertx.core.Vertx;
    import java.util.Map;
    import org.folio.service.RecordService;
    import org.folio.service.RecordServiceImpl;
    import org.folio.service.spi.RecordServiceFactory;
    
    public class RecordServiceFactoryImpl implements RecordServiceFactory {
    
      @Override
      public RecordService create(Vertx vertx) {
        return RecordServiceImpl.createForSingleTable(
          vertx, Map.of("po_line", "po_line", "purchase_order", "purchase_order"));
      }
    }
    ```

2. Make the implementation available by updating the `META-INF/services/org.folio.service.
   RecordServiceFactory` file to contain the fully qualified name of the implementation class
  
    ```
    org.folio.services.record.RecordServiceFactoryImpl
    ```

If neither of the above implementations are suitable, the steps above can also be followed to use a 
different implementation.

## Issue tracker

See project [FCFIELDS](https://issues.folio.org/browse/FCFIELDS)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker/).

## Code analysis
[SonarQube analysis](https://sonarcloud.io/dashboard?id=org.folio%3Afolio-custom-fields).
