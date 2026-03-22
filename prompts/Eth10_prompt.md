# 10Mbit Ethernet MAC/PHY

## Overview

Create a 10Mbit Ethernet MAC/PHY implementation in Chisel. The module should support basic Ethernet frame transmission and reception at 10Mbps speed. 
The implementation should include the following features:
 - Support for 10Mbps Full-duplex operation
 - Basic frame handling (preamble, SFD, payload, CRC)
 - Encode and decode from Manchester encoding to MII
 - Receive from MII interface and convert to Decoupled internal interface
 - Produce statistics vector for each sent and received frame

## Interface

The Ethernet PHY implementation should have the following interface:
```scala
class EthMII extends Bundle {
    val tx_clk = Output(Clock()) // Transmit clock (2.5MHz for 10Mbps)
    val tx_en = Output(Bool())   // Transmit enable
    val txd = Output(UInt(4.W))  // Transmit data (4 bits for MII)
    
    val rx_clk = Input(Clock())   // Receive clock (2.5MHz for 10Mbps)
    val rx_dv = Input(Bool())    // Receive data valid
    val rxd = Input(UInt(4.W))   // Receive data (4 bits for MII)
    
    val col = Input(Bool())      // Collision detected
    val crs = Input(Bool())      // Carrier sense
}

class EthPhy10 extends Bundle {
    // MII Interface
    val mii = Flipped(new EthMII) // MII interface for PHY connection
    
    val tx_p = Output(Bool())   // Transmit data line (Manchester encoded)
    val tx_n = Output(Bool())   // Transmit data line (Manchester encoded)
    val tx_en = Output(Bool())   // Transmit enable for Manchester encoding
    
    val rx_p = Input(Bool())    // Receive data line (Manchester encoded)
    val rx_n = Input(Bool())    // Receive data line (Manchester encoded)
}
```

The MAC should have the following interface:
```scala
class StatVector extends Bundle {
    val frameLength = UInt(16.W) // Length of the frame in bytes
    val crcError = Bool()        // Indicates if a CRC error was detected
    val collision = Bool()       // Indicates if a collision was detected during transmission
}

class EthMac10 extends Bundle {
  val mii = new EthMII // MII interface for PHY connection
  val rxd = Deoupled(UInt(8.W)) // Received frame data (8 bits)
  val txd = Flipped(Decoupled(UInt(8.W))) // Transmit

    val statVector = Output(ValidIO(new StatVector)) // Statistics vector for sent and received frames
```

## Code Generation

Create modules in package eth10.  The code should consist of at least the three following modules:
    - `EthPhy10` - The Ethernet PHY module that handles Manchester encoding/decoding and MII interface
    - `EthMac10` - The Ethernet MAC module that handles frame processing and MII interface
    - `EthTop10` - A top-level module that instantiates both the MAC and PHY and connects them together.

Create other intermediate and shared modules as needed, such as for CRC calculation and Manchester encoding/decoding.
The code should operate with a clock which is a multiple of 10 Mhz, with a divide-down parameter.  The source clock
will be at least 50Mhz.