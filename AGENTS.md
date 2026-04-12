# Agent information for work flows

## File and Directory Structure

A design block consists of one or more Modules which are organized to accomplish
a given design goal.  A design block has well defined interfaces and input and
output products.

Each design block resides in its own directory, with matching package name.  The 
directory should contain the files

 - ARCHITECTURE.md
 - "design block".scala
 - "design block"Types.scala

Each design block has a corresponding test directory with the same package name,
which contains its unit tests.

## Planning Steps

The planning agent should create the architecture file, which defines the input 
and output products of the block, as well as the design features.  The architecture
file should document how input products are transformed to output products.

The design agent creates Modules from the architecture, following the general design
principles listed below.  The design agent should split the design block should create
child Modules where needed to limit complexity in any one module.

The verification agent should read the architecture file to determine the interfaces 
to the block and the features needed to be tested.  The verification agent should create 
a test for each feature defined in the design block.

## Design Principles

Designs should be inplemented following the principles outlined below, which represent
best practices for logic design.

### Pipeline Stages

The design should be divided into pipeline stages, which contains combinatorial logic 
followed by a storage element (flop).  The design should separate the next-stage logic
to compute the output product from the flow control logic where possible.

### State Machines

If a pipeline state can be in different stages, a stage machine should be created for
the stage.  State machines should use only signals from the current stage.  If 
information from a previous stage is needed, it should be combined with the output
product for the previous stage.

### Decoupled Interfaces

Stages should be connected with Decoupled interfaces if any stage or the final output
can block.  Stages should use DCOutput blocks to register their outputs to ensure 
proper flow control implementation.

Where a stage has multiple input or output interfaces, it should create a global 
allReady signal from all input valid signals and all output ready signals, when
all input interfaces are needed.  This allReady signal is used to drive input
ready and output valid signals.
