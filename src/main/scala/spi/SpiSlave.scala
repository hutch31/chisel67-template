package spi

import chisel3._
import chisel3.util._

/** SPI Slave implementation.
  *
  * Implements a SPI slave in Mode 0 (CPOL=0, CPHA=0).  The system clock must
  * be at least 4× the SPI clock frequency so that edges can be reliably
  * detected via oversampling.
  *
  * @param width  Number of data bits transferred per SPI clock cycle on the
  *               mosi/miso lines (1, 2, 4, or 8).  A "word" is always 8 bits;
  *               `width` bits are shifted per SPI clock edge so a full word
  *               takes (8/width) SPI clocks.
  */
class SpiSlave(width: Int = 1) extends Module {
  require(Seq(1, 2, 4, 8).contains(width), s"width must be 1, 2, 4, or 8; got $width")

  // Number of SPI clock cycles needed to transfer one 8-bit word
  private val bitsPerWord = 8
  private val cyclesPerWord = bitsPerWord / width

  val io = IO(new Bundle {
    val spi      = new SPIInterface(width)
    val rxData   = Decoupled(UInt(8.W))         // received byte to internal logic
    val txData   = Flipped(Decoupled(UInt(8.W))) // byte to transmit to master
    val lsbFirst = Input(Bool())                 // true → LSB-first bit order
  })

  // -----------------------------------------------------------------------
  // Stage 1 – Input synchronization & edge detection
  // -----------------------------------------------------------------------

  // 3-stage shift register for sclk – provides 2-FF synchronizer plus one
  // extra stage for edge detection.
  val sclkSync = RegInit(0.U(3.W))
  sclkSync := Cat(sclkSync(1, 0), io.spi.sclk)

  val sclkRise = !sclkSync(2) && sclkSync(1)  // rising  edge (Mode 0: sample)
  val sclkFall =  sclkSync(2) && !sclkSync(1) // falling edge (Mode 0: shift out)

  // 2-FF synchronizer for chip-select (active low → inverted to csActive)
  val csSync = RegInit(VecInit(Seq.fill(2)(false.B)))
  csSync(0) := !io.spi.cs        // invert: true when selected
  csSync(1) := csSync(0)
  val csActive = csSync(1)

  // Detect the start of a new transfer (rising edge of csActive)
  val csActivePrev = RegNext(csActive, false.B)
  val csStart      = csActive && !csActivePrev

  // -----------------------------------------------------------------------
  // Stage 2 – Shift registers, bit counter, and flow control
  // -----------------------------------------------------------------------

  // Bit counter: counts SPI clock cycles within the current word
  val bitCnt = RegInit(0.U(log2Ceil(cyclesPerWord + 1).W))

  // RX shift register (8 bits accumulates one full word)
  val rxShift = RegInit(0.U(8.W))

  // TX shift register (8 bits, loaded when cs goes active or word is done)
  val txShift = RegInit(0.U(8.W))

  // Valid flag for rxData output
  val rxValid = RegInit(false.B)
  val rxReg   = RegInit(0.U(8.W))

  // TX ready tracking: have we consumed the current txData word?
  val txReady = RegInit(false.B) // true when txShift is loaded with valid data

  // ------------------------------------------------------------------
  // TX shift register loading
  // ------------------------------------------------------------------
  // Accept new TX data when:
  //   - a transfer starts (csStart), or
  //   - we have just finished shifting out the last bit of the current word
  //     and there is new data available
  val txLoad = Wire(Bool())
  txLoad := false.B

  io.txData.ready := false.B

  when (csStart) {
    // Load TX shift register at the start of a transfer
    when (io.txData.valid) {
      txShift            := io.txData.bits
      io.txData.ready    := true.B
      txReady            := true.B
    } .otherwise {
      txShift  := 0.U
      txReady  := false.B
    }
    bitCnt := 0.U
    txLoad := true.B
  }

  when (!csActive) {
    bitCnt  := 0.U
    txReady := false.B
  }

  // ------------------------------------------------------------------
  // RX: sample mosi on rising edge of sclk while cs is active
  // ------------------------------------------------------------------
  when (sclkRise && csActive) {
    rxShift := Mux(
      io.lsbFirst,
      Cat(io.spi.mosi, rxShift(7, width)),  // LSB-first: shift in at MSB, data builds from left
      Cat(rxShift(7 - width, 0), io.spi.mosi) // MSB-first: shift in at LSB end
    )

    val nextBitCnt = bitCnt + 1.U
    bitCnt := nextBitCnt

    when (nextBitCnt === cyclesPerWord.U) {
      // Word complete – latch into output register
      rxValid := true.B
      rxReg   := Mux(
        io.lsbFirst,
        Cat(io.spi.mosi, rxShift(7, width)),
        Cat(rxShift(7 - width, 0), io.spi.mosi)
      )
      bitCnt  := 0.U

      // Load next TX word now that one word boundary has been crossed
      when (io.txData.valid) {
        txShift         := io.txData.bits
        io.txData.ready := true.B
        txReady         := true.B
      } .otherwise {
        txShift := 0.U
        txReady := false.B
      }
    }
  }

  // De-assert rxValid once consumed
  when (rxValid && io.rxData.ready) {
    rxValid := false.B
  }

  // ------------------------------------------------------------------
  // TX: drive miso and shift on falling edge of sclk while cs active
  // ------------------------------------------------------------------
  when (sclkFall && csActive) {
    when (io.lsbFirst) {
      txShift := Cat(0.U(width.W), txShift(7, width))
    } .otherwise {
      txShift := Cat(txShift(7 - width, 0), 0.U(width.W))
    }
  }

  // miso is combinatorially driven from the shift register
  io.spi.miso := Mux(
    io.lsbFirst,
    txShift(width - 1, 0),    // LSB-first: drive low bits
    txShift(7, 8 - width)     // MSB-first: drive high bits
  )

  // ------------------------------------------------------------------
  // rxData output (registered, Decoupled)
  // ------------------------------------------------------------------
  io.rxData.valid := rxValid
  io.rxData.bits  := rxReg
}
