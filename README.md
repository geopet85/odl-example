# Opendaylight Example
This is a simple Opendaylight example, performing some simple operations with the SDN Controller. 

It exposes an API to add applications, uses the registry to store them, configures flow rules, and does link monitoring and topology functions.

## Prerequisites
To build the project, you have to setup Java and Maven first, according to the [instructions](https://wiki.opendaylight.org/view/GettingStarted:Development_Environment_Setup).

##Build & Run
<code>mvn clean install -DskipTests</code>

<code>cd karaf/target/assembly/bin</code>

<code>./karaf</code>

To check that our modules are running, use the following commands in the Opendaylight console:

<code>bundle:list | grep odlexample</code>  (odlexample-impl and odlexample-api should be active)

##Simple Example
Start the Opendaylight with the previous commands.

Install Mininet and run a simple topology, e.g. 1 switch, 2 hosts, by executing <code>sudo mn --controller remote,ip="sdn-controller-ip" --topo single, 2</code>

Browse the [ODL user interface](http://localhost:8181/index.html) and connect with admin/admin credentials.

Browse the [Yang UI page](http://localhost:8181/index.html#/yangui/index), expand all in API tab and find odlexample API.

In *operations* bullet, add an application. Then in *operational* bullet, check whether it has been stored in the registry.

In the Opendaylight console, execute <code>log:display | grep odlexample</code> to check that certain functions work properly when you use the REST API.

In the Mininet console, execute <code>sh ovs-ofctl dump-flows s1</code> to check that a new flow rule is inserted.
