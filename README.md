# chisel67-template

## SPI Slave (Chisel)

This workspace now includes a SPI slave implementation in `src/main/scala/spi/SpiSlave.scala`.

Implemented features:
- Slave mode only
- Mode 0 timing (`CPOL = 0`, `CPHA = 0`)
- Lane width parameter `width = 1 | 2 | 4 | 8`
- MSB-first and LSB-first transfer order (`io.lsbFirst`)
- Oversampled internal clock assumption (`chisel clock >= 4x SPI clock`)

### Module I/O

`SpiSlave(width: Int)` exposes:
- SPI pins via `io.spi` (`sclk`, `mosi`, `miso`, `cs`)
- `io.txData`, `io.txValid`, `io.txReady` for one-byte TX buffering
- `io.rxData`, `io.rxValid` for received byte output
- `io.busy` and `io.misoEnable` status

### Files

- `src/main/scala/spi/SpiSlave.scala` - SPI interface bundle and module
- `src/main/scala/spi/GenerateSpiSlave.scala` - Verilog generator app
- `src/test/scala/spi/SpiSlaveSpec.scala` - chiseltest testbench

### Quick start

Run tests:

```powershell
sbt test
```

Generate Verilog:

```powershell
sbt "runMain spi.GenerateSpiSlave"
```
