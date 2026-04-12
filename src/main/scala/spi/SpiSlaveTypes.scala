package spi

import chisel3._
import chisel3.util._

/** Physical SPI interface bundle.
  *
  * @param width number of data bits per clock cycle on mosi/miso (1, 2, 4, or 8)
  */
class SPIInterface(width: Int) extends Bundle {
  val sclk = Input(Bool())         // SPI clock (from master)
  val mosi = Input(UInt(width.W))  // Master Out Slave In
  val miso = Output(UInt(width.W)) // Master In Slave Out
  val cs   = Input(Bool())         // Chip Select (active low)
}
