\# Scala Test Directory

This directory holds Scala unit tests

## Chiseltest Testbenches

Chiseltest testbenches use the Scala test framework to create simple unit tests.
One of the key advantages of Chiseltest (for Windows users) is that it does not 
require a separate Verilog simulator installation. It uses the built-in Treadle 
simulator, which is included as a dependency in the project.

## Generating Waveforms

Copilot-generated unit tests will generally not have VCD generation on.  To add
waveform generation to a test, you need to add an annotation to the test command.

If your test is originally defined as:

```scala
test(new MyModule(2)) {
...
```

You can modify it to include VCD generation like this:

```scala
test(new MyModule(2)).withAnnotations(Seq(WriteVcdAnnotation)) {
...
```
This will enable VCD waveform generation for that specific test.
You can use a free waveform viewer like GTKWave to open the generated VCD files.



