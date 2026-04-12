package spi

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

/** Test helpers for driving the SPI bus */
object SpiHelper {

  /** Drive one full 8-bit word over SPI at the given width.
    *
    * MISO is sampled immediately after sclk is asserted high (before any clock
    * step), matching Mode-0 semantics where the slave pre-drives MISO from the
    * previous falling edge and the master captures on the rising edge.
    *
    * @param dut       the SpiSlave DUT
    * @param data      8-bit word to send (MOSI)
    * @param width     number of bits per SPI clock cycle
    * @param lsbFirst  true → send LSB first
    * @param halfPeriod system-clock cycles per SPI half-period (must be ≥ 4 for
    *                   the 3-stage synchronizer to detect edges reliably)
    * @return collected MISO bits assembled into an 8-bit word
    */
  def transferWord(
      dut: SpiSlave,
      data: Int,
      width: Int,
      lsbFirst: Boolean,
      halfPeriod: Int = 4
  ): Int = {
    val cyclesPerWord = 8 / width
    var misoWord = 0

    for (cycle <- 0 until cyclesPerWord) {
      // Extract the slice to send on MOSI
      val slice = if (lsbFirst) {
        (data >> (cycle * width)) & ((1 << width) - 1)
      } else {
        (data >> (8 - (cycle + 1) * width)) & ((1 << width) - 1)
      }

      // Set MOSI and step through the low half-period.
      // The falling edge from the previous high period is detected here.
      dut.io.spi.mosi.poke(slice.U)
      dut.clock.step(halfPeriod)

      // Rising edge: assert sclk and immediately capture MISO (combinatorial)
      // before the slave's synchronizer has clocked in the new sclk state.
      // This mirrors Mode-0 master behaviour: sample at the rising edge instant.
      dut.io.spi.sclk.poke(true.B)
      val misoSlice = dut.io.spi.miso.peek().litValue.toInt

      if (lsbFirst) {
        misoWord = misoWord | (misoSlice << (cycle * width))
      } else {
        misoWord = (misoWord << width) | misoSlice
      }

      // Step through the high half-period so the slave detects the rising edge.
      dut.clock.step(halfPeriod)

      // Falling edge: lower sclk (slave will shift TX on next low half-period)
      dut.io.spi.sclk.poke(false.B)
    }
    misoWord
  }

  /** Assert chip-select (active low) and wait for synchronizers to settle. */
  def csAssert(dut: SpiSlave): Unit = {
    dut.io.spi.cs.poke(false.B)
    dut.clock.step(4) // allow 2-FF synchronizer plus margin
  }

  /** Deassert chip-select. */
  def csDeassert(dut: SpiSlave): Unit = {
    dut.io.spi.cs.poke(true.B)
    dut.clock.step(4)
  }
}

class SpiSlaveTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "SpiSlave"

  // -----------------------------------------------------------------------
  // Feature 1: Basic RX – MSB-first, width=1
  // -----------------------------------------------------------------------
  it should "receive a byte MSB-first with width=1" in {
    test(new SpiSlave(1)) { dut =>
      dut.io.spi.sclk.poke(false.B)
      dut.io.spi.cs.poke(true.B)
      dut.io.lsbFirst.poke(false.B)
      dut.io.txData.valid.poke(false.B)
      dut.io.rxData.ready.poke(false.B) // not consuming yet
      dut.clock.step(2)

      SpiHelper.csAssert(dut)

      val txByte = 0xA5
      SpiHelper.transferWord(dut, txByte, width = 1, lsbFirst = false)
      dut.clock.step(4) // allow rxValid register to propagate

      dut.io.rxData.valid.expect(true.B)
      dut.io.rxData.bits.expect(txByte.U)

      // Acknowledge
      dut.io.rxData.ready.poke(true.B)
      dut.clock.step(1)
      dut.io.rxData.valid.expect(false.B)

      SpiHelper.csDeassert(dut)
    }
  }

  // -----------------------------------------------------------------------
  // Feature 2: Basic RX – LSB-first, width=1
  // -----------------------------------------------------------------------
  it should "receive a byte LSB-first with width=1" in {
    test(new SpiSlave(1)) { dut =>
      dut.io.spi.sclk.poke(false.B)
      dut.io.spi.cs.poke(true.B)
      dut.io.lsbFirst.poke(true.B)
      dut.io.txData.valid.poke(false.B)
      dut.io.rxData.ready.poke(false.B)
      dut.clock.step(2)

      SpiHelper.csAssert(dut)

      val txByte = 0x3C
      SpiHelper.transferWord(dut, txByte, width = 1, lsbFirst = true)
      dut.clock.step(4)

      dut.io.rxData.valid.expect(true.B)
      dut.io.rxData.bits.expect(txByte.U)

      dut.io.rxData.ready.poke(true.B)
      dut.clock.step(1)
      dut.io.rxData.valid.expect(false.B)

      SpiHelper.csDeassert(dut)
    }
  }

  // -----------------------------------------------------------------------
  // Feature 3: Basic RX – MSB-first, width=4 (two SPI cycles per byte)
  // -----------------------------------------------------------------------
  it should "receive a byte MSB-first with width=4" in {
    test(new SpiSlave(4)) { dut =>
      dut.io.spi.sclk.poke(false.B)
      dut.io.spi.cs.poke(true.B)
      dut.io.lsbFirst.poke(false.B)
      dut.io.txData.valid.poke(false.B)
      dut.io.rxData.ready.poke(false.B)
      dut.clock.step(2)

      SpiHelper.csAssert(dut)

      val txByte = 0xDE
      SpiHelper.transferWord(dut, txByte, width = 4, lsbFirst = false)
      dut.clock.step(4)

      dut.io.rxData.valid.expect(true.B)
      dut.io.rxData.bits.expect(txByte.U)

      dut.io.rxData.ready.poke(true.B)
      dut.clock.step(1)
      dut.io.rxData.valid.expect(false.B)

      SpiHelper.csDeassert(dut)
    }
  }

  // -----------------------------------------------------------------------
  // Feature 4: Back-to-back transfers in one CS assertion
  // -----------------------------------------------------------------------
  it should "receive two consecutive bytes in a single CS assertion" in {
    test(new SpiSlave(1)) { dut =>
      dut.io.spi.sclk.poke(false.B)
      dut.io.spi.cs.poke(true.B)
      dut.io.lsbFirst.poke(false.B)
      dut.io.txData.valid.poke(false.B)
      dut.io.rxData.ready.poke(false.B)
      dut.clock.step(2)

      SpiHelper.csAssert(dut)

      val byte1 = 0xAB
      SpiHelper.transferWord(dut, byte1, width = 1, lsbFirst = false)
      dut.clock.step(4)

      dut.io.rxData.valid.expect(true.B)
      dut.io.rxData.bits.expect(byte1.U)
      // Acknowledge first byte before second word completes
      dut.io.rxData.ready.poke(true.B)
      dut.clock.step(1)
      dut.io.rxData.ready.poke(false.B)

      val byte2 = 0xCD
      SpiHelper.transferWord(dut, byte2, width = 1, lsbFirst = false)
      dut.clock.step(4)

      dut.io.rxData.valid.expect(true.B)
      dut.io.rxData.bits.expect(byte2.U)

      dut.io.rxData.ready.poke(true.B)
      dut.clock.step(1)
      dut.io.rxData.valid.expect(false.B)

      SpiHelper.csDeassert(dut)
    }
  }

  // -----------------------------------------------------------------------
  // Feature 5: TX – master reads back a byte sent by the slave
  // -----------------------------------------------------------------------
  it should "transmit a byte to the master MSB-first with width=1" in {
    test(new SpiSlave(1)) { dut =>
      dut.io.spi.sclk.poke(false.B)
      dut.io.spi.cs.poke(true.B)
      dut.io.lsbFirst.poke(false.B)
      dut.io.rxData.ready.poke(false.B)

      // Pre-load a byte to transmit
      val txByte = 0x55
      dut.io.txData.valid.poke(true.B)
      dut.io.txData.bits.poke(txByte.U)
      dut.clock.step(2)

      SpiHelper.csAssert(dut)

      // Deassert txData.valid after the word is loaded on csStart so the
      // design does not reload txShift when the last RX word completes.
      dut.io.txData.valid.poke(false.B)

      // Drive MOSI as 0 – we only care about MISO here
      val misoWord = SpiHelper.transferWord(dut, 0x00, width = 1, lsbFirst = false)
      dut.clock.step(2)

      assert(misoWord == txByte, s"Expected MISO 0x${txByte.toHexString}, got 0x${misoWord.toHexString}")

      SpiHelper.csDeassert(dut)
    }
  }

  // -----------------------------------------------------------------------
  // Feature 6: Chip-select deassert resets the bit counter
  // -----------------------------------------------------------------------
  it should "reset correctly when CS is deasserted mid-transfer" in {
    test(new SpiSlave(1)) { dut =>
      dut.io.spi.sclk.poke(false.B)
      dut.io.spi.cs.poke(true.B)
      dut.io.lsbFirst.poke(false.B)
      dut.io.txData.valid.poke(false.B)
      dut.io.rxData.ready.poke(false.B)
      dut.clock.step(2)

      // Start a transfer, send only 3 bits then deassert
      SpiHelper.csAssert(dut)
      for (_ <- 0 until 3) {
        dut.io.spi.mosi.poke(1.U)
        dut.clock.step(4)
        dut.io.spi.sclk.poke(true.B)
        dut.clock.step(4)
        dut.io.spi.sclk.poke(false.B)
      }

      // Deassert CS – no valid output should appear
      SpiHelper.csDeassert(dut)
      dut.io.rxData.valid.expect(false.B)

      // Now do a full clean transfer
      SpiHelper.csAssert(dut)
      val fullByte = 0xFF
      SpiHelper.transferWord(dut, fullByte, width = 1, lsbFirst = false)
      dut.clock.step(4)

      dut.io.rxData.valid.expect(true.B)
      dut.io.rxData.bits.expect(fullByte.U)

      dut.io.rxData.ready.poke(true.B)
      dut.clock.step(1)
      dut.io.rxData.valid.expect(false.B)

      SpiHelper.csDeassert(dut)
    }
  }
}

