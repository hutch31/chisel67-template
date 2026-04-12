# SPI Slave Architecture

## Overview

This design block implements a SPI (Serial Peripheral Interface) slave device in Chisel.
The implementation supports SPI Mode 0 (CPOL=0, CPHA=0), parameterized data width
(1, 2, 4, or 8 bits), and configurable bit order (MSB-first or LSB-first).

## Input Products

- `sclk`: SPI clock from master (oversampled by the system clock at ≥4X frequency)
- `mosi`: Master Out Slave In data line, `width` bits wide
- `cs`: Chip select, active low
- `lsbFirst`: Runtime configuration to select LSB-first (true) or MSB-first (false) ordering

## Output Products

- `miso`: Master In Slave Out data line, `width` bits wide
- `rxData`: Decoupled output carrying received bytes/words from the master
- `txData`: Decoupled input accepting bytes/words to send to the master

## Design Features

1. **Oversampled clock edge detection**: The SPI clock is oversampled using the system
   clock. Rising and falling edges of `sclk` are detected via a 3-stage synchronizer/
   shift register to safely cross the clock domain.

2. **Chip select synchronization**: `cs` (active low) is synchronized to the system
   clock through a 2-flop synchronizer.

3. **Bit counter**: Tracks how many bits have been shifted in/out per transfer word.
   Resets when `cs` is deasserted.

4. **Shift register (receive)**: On each detected rising edge of `sclk` (while `cs` is
   low), the incoming `mosi` data is shifted into a receive shift register. When
   `8/width` SPI clock cycles have elapsed (i.e., all 8 bits of a word have been
   received), the word is presented on `rxData`.

5. **Shift register (transmit)**: The `txData` word is loaded into a transmit shift
   register when `cs` goes low (start of transfer) or when the previous word is
   exhausted. `miso` is driven from the MSB (or LSB) of the transmit shift register
   and shifted on each falling edge of `sclk`.

6. **Bit/word ordering**: Both the receive and transmit shift registers respect the
   `lsbFirst` parameter to shift in/out from the appropriate end.

## Pipeline Stages

### Stage 1 – Input Synchronization (combinatorial + flop)

- Synchronize `sclk` and `cs` to the system clock domain using shift registers.
- Detect rising edge (`sclkRise`) and falling edge (`sclkFall`) of `sclk`.
- Detect the deasserted-to-asserted transition of `cs` (start of transfer).

### Stage 2 – Shift and Control (combinatorial + flop)

- On `sclkRise` and active `cs`:  shift `mosi` into the RX shift register.
- On `sclkFall` and active `cs`: shift the TX shift register; drive `miso`.
- Increment the bit counter on each bit clock edge.
- When the bit counter reaches `cyclesPerWord` (= `8/width`), assert `rxData.valid` and load next TX word.

## Interfaces

```
SpiSlave (top-level Module)
  spi  : SPIInterface(width)     – physical SPI pins
  rxData : Decoupled(UInt(8.W))  – received data to internal logic
  txData : Flipped(Decoupled(UInt(8.W))) – data to transmit to master
  lsbFirst : Input(Bool())       – bit ordering control
```
