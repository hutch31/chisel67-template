# chisel67-template

This is a basic template for a Chisel6.7 based project, with hooks for Chiseltest.  This project is intended as a
bare starting point for experimenting with code generation with Copilot.

## IntelliJ Setup

IntelliJ will need the Scala and Github Copilot plugins for this to work correctly.  It will also need a Java JDK in order to compile
the Scala files.  firtool is not needed for any of the unit tests, as the Treadle (Scala-native) simulator is good enough for short 
tests.

## Testing Results

### SPI

With the included prompt, SPI generation took about 2-3 minutes.  The agent needed to interate about 3 times to get to passing unit
tests.

### Ethernet

This made the agent work significantly harder.  Initial code generation from prompt was about 5 minutes, and took 2 iterations to
get to a failing unit test run.  At the failing unit test the tool reached its maximum prompt limit.  Trying 'continue'.

The agent has now interated on the failing unit test 4 times.  It has turned on waveform capture to examine the dump file to see
what the issue is.  Hit maximum attempts again.

Intervened to help the agent out, told it to focus on testing the PHY first. (15:22)  Hit max attempts again at 15:27, Continued.
Realized it is failing because the interface has a generated clock, which Treadle cannot handle, and the agent is unable to see
the issue.  Redefined the interface so that it uses enables rather than a clock (15:37).  PHY-specific test now passes at 15:45.
Added new test for RX, passing at 15:50.  Increased size of test to 8B each for TX/RX (15:54)

Changed to testing MAC standalone.  Started with TX test, 2 iterations to compiling test, 6 more to passing test (16:16)
Added RX test, 3 more interations to running test (16:24)

Now that MAC and PHY tests are passing, ask agent to build complete test for TX.  Agent hs iterated many times on trying to get this
test to pass and is not able to converge.  Finished at 17:21.

Analysis:  The scope was somewhat of a problem for this, but the agent really seem to get hung up on debugging; it frequently iterated
without seeming to be able to make progress.  There may have been some Windows issues involved, as it had problems dumping output and
then stating the log file was not present.

This would have benefited first from a better breakdown of the architecture itself, but also with some hints on how to build drivers
and monitors that had text output it was able to interpret.






