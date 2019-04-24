# BPM Integration
This application is intended to bridge an IBM MQ queue and a BPM UCA

##Pre Configuration

* from the IBM BPM Process Centre WAS Admin console /ibm/console
  Navigate to `Resources > JMS > Queues`
* Select the correct scope (usually `Cluster=SingleCluster`) and configure a new queue connection to a running Queue manager (it is assumed that you have already created the persistent ***out*** and ***in*** queues)
* Save the JNDI queue name for later
* Navigate to `Resources > JMS > Activation Specifications`
* Create a new MQ Activation Specification
* Save the JNDI AS name for later

```
NOTE
Don't forget after deploying of making changes in the WAS console to Synchronize the nodes

System Integration > Nodes
```

## Getting started
* Log onto the IBM BPM Process Center and install(import) the TWX file `/docs/example/MQ_Integration_App - V1.0.1.twx`
* Open the Process Application and edit the MQ connection settings in services:
  * `Credit Check Service > MQ Put > Data Mapping`
  * `MQ Get Service > MQ Get Credit Score > Data Mapping`

## Running
* Start some new instances by editing the process task ***Gather Customer Info*** > Properties > *Implementation*

  Give each new instance a unique `tw.local.customerInfo.SSN` number and unique `tw.local.customerInfo.name` customer name.
  
* Switch to the Inspector to see each process running. 

### Running the manual MQ Get
* Make sure the EAR application in the was console is stopped (this application)
* Put a message in the IncomingQ queue with IBM MQ Explorer to mimic a Credit Score service provider that places a
  message in the queue: in the format of a comma-separated message `123456789,620` message in the
  queue. In this example, 123456789 is the customer SSN and 620 is the credit score. 
* The customer SSN is set in the Gather
  Customer Info script in the beginning of the process. You can update the script to provide a different SSN, but then, make sure
  to use a different message to IBM MQ when testing.
* Run the IBM MQ Get Service service from Process Designer. You can see the token for the process instance moved to the
  Approval human service because the business rule determines that if the credit score is less than 650, a manager approval is
  needed.


### Running the activation spec listener 
* Start a new BPD instance for the testing by updating the Gather Customer Info script to provide a different
customerSSN, for example, 987654321. 
* Put a message in the IncomingQ queue as 98765432,700 (if you deployed the JMSUCATest.ear) and the message waits to get picked up the MDB. In the
administrative console, start the JMSUCATest application that you installed earlier. Back in Process Designer, you see that the
process instance moved through the flow to the end node. Because the credit score is 700, the manager approval step is not
needed.
* If you deployed this application (i.e. BPMUCAMQConnectionApplicationEAR) then the message that must get place on the queue is in the format of an XML message
(this is more generic for all processes to receive events)

```
<eventmsg>
  <event processApp="MQAPP" ucaname="Credit Check UCA">creditScoreMessage</event> 
  <parameters>  
	<parameter> 
           <key>customerSSN</key> 
           <value>1234</value> 
     </parameter>
     <parameter>
           <key>creditScore</key> 
           <value>620</value>
    </parameter>
 </parameters>
</eventmsg>
```

## XML Message Structure
The message that you post to the Event Manager must contain an event name (the message event ID generated when you create an undercover agent) and it can contain parameter names and values in key-value pairs. (The topic Creating and configuring an undercover agent for a message event describes the message event ID that you should use for the event name.) The message must be structured as XML as shown in the following example:

```
<eventmsg> 
<!-- The process app acronym and event name are required: The snapshot and UCA name are optional --> 
<event processApp="[acronym]" snapshot="[snapshot_name]" ucaname="[UCA_name]">[event_name]</event> 
<!--Optional: The name of the queue for the event to run in--> 
<queue>[queue name]</queue> 
<!--Any parameters the UCA may require-- 
    <parameters> 
        <parameter> 
            <key>param1</key> 
            <value><![CDATA[value1]]> </value>
        </parameter> 
    </parameters> 
</eventmsg>
```
If you do not include the snapshot name in the message, the Event Manager uses the default snapshot on the target Process Server for start message events. For intermediate message events, if you do not include the snapshot name in the message, all active snapshots receive events.

If the value of the <value> element contains XML elements or similar content, you need to wrap the value in a CDATA tag as shown in the preceding example. For information about passing parameter values for each complex business object (variable type), see the following section.

#### Example of passing a Structured type
```
<eventmsg> 
    <event processApp="[acronym]" snapshot="[snapshot_name]" ucaname="[UCA_name]">[event name]</event> 
    <parameters> 
        <parameter> 
            <key>customerParam</key> 
            <value> 
                <Name>John</Name> 
                <Description>John Description</Description> 
                <Age>30</Age> 
            </value> 
        </parameter> 
    </parameters> 
</eventmsg>
```