<img align="left" width=150px src="https://github.com/hsu-aut/olif/blob/documentation/images/images/Olif-Logo.png" alt="Olif-Logo"/>

# Olif - Ontology mapping language for data interchange formats
<br><br>

Olif is a generic mapping language to define custom mappings that transform RDF data into data interchange formats such as XML or JSON. Based on the ideas of [RML](https://rml.io/).

## About
You took time and efforts and created a semantic model for all your information which may be contained in heterogeneous data sources. But then you need to use some slice of those information and import it into a legacy tool. Of course this legacy tool only supports XML or JSON imports. How do you get the relevant parts of your semantic model in the form that your legacy tool accepts? 
With Olif you can define mapping rules that transform selected parts of your ontology into XML or JSON with a structure you define.

## Install

### Download and run
The easiest way to use Olif is by running it as a CLI. Download the latest version as a jar and use it like this:
`java -jar .\olif-cli-0.0.1-SNAPSHOT-jar-with-dependencies.jar -m <path to mapping file> -o <path to output file>`

Make sure to set the two required arguments:
- `-m`: Path to a file with valid mapping definitions (see https://github.com/hsu-aut/olif#usage)
- `-o`: Path to an output file. In case this path doesn't yet exist, a file will be created. If it exists, the mapping output will be inserted into this file.

### As a Maven dependency
🚧 Documentation coming soon 🚧

### Compile from source
Olif is a multi-module Maven project. To compile all the modules, simply clone the whole repository and execute `mvn clean install` from the root of the project.
You can also build a single module (e.g. cli) -  in order to do that just run the same command in the corresponding module's directory.

## Usage
Define mappings using the Olif mapping language in a so-called _mapping model_. We recommend to write a Turtle file, but other RDF serialization formats should work just fine, too. See this example:

```turtle

@prefix ol: <http://www.hsu-hh.de/aut/ontologies/olif#>.
# Your other prefix definitions
...
<#ParameterMapping> a ol:DataMap;
  ol:ontologicalSource [
    ol:source "parameters.ttl";
	  ol:sourceType ol:File;
    ol:queryLanguage ql:Sparql;
    ol:query 
      "PREFIX ex: <http://www.hsu-hh.de/aut/ontologies/example#> 
      SELECT ?parameterName ?parameterValue WHERE {
        ?parameter a ex:Parameter.
        ex:RobotConfiguration_ABC a ex:RobotConfiguration;
        ex:hasParameter ?parameter.
        ?parameter ex:hasName ?parameterName;
          ex:hasValue ?parameterValue.
      }"
  ];
  ol:referenceFormulation "XPath";
  ol:container "/parameters";
  ol:snippet 
   "<ParamWithValue>
      <name>length_${parameterName}</name>
      <typeCode>mm</typeCode>
      <value >${parameterValue} mm</value>
    </ParamWithValue>".
    
# Additional mappings...
```

A `DataMap` is the class for all Olif mapping definitions. Every `DataMap` has to have an `ontologicalSource` which specifies the source data to be mapped:
- `source` defines a file path to a source ontology or the URL of a SPARQL endpoint
- `sourceType` is used to specify whether a `File` or `SparqlEndpoint` is used
- `queryLanguage`defines the language used to retrieve information from the `source`. We currently support only `Sparql`, but will maybe support other ways of retrieving data in the future
- `query` contains the actual query string used to retrieve data from the source. Results of this query can be used to insert dynamic data into the mapping output. In the above example, the variable `?parameterName` and `?parameterValue` are used inside the `snippet`.

While `ontologicalSource` is used to define the source of a mapping, `container` and `snippet` declare the output structure. The string connected to a `DataMap` via `container` is a search expression in the output document that determines into which existing structure the mapping is executed. In case this structure doesn't exist, it is created. In the example above, an XML tag `parameters` is searched. The `container` string may contain variables of the SPARQL Query so that results of the query are inserted before searching.

The `snippet` defines the actual mapping output which is inserted into the container. It is a string that may also contain variables of the SPARQL query. In the example above, a parameter structure is defined with a dynamic name and value.
