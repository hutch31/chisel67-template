# Agent information for work flows

## File and Directory Structure

A design block consists of one or more Modules which are organized to accomplish
a given design goal.  A design block has well defined interfaces and input and
output products.

Each design block resides in its own directory, with matching package name.  The 
directory should contain the files

 - ARCHITECTURE.md
 - <design block>.scala
 - <design block>Types.scala

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

Design of a block 