package com.colingodsey.scalaminer.drivers

import javax.usb.{UsbDevice, UsbConst}
import akka.actor._
import com.colingodsey.scalaminer.usb._
import com.colingodsey.scalaminer._
import scala.concurrent.duration._
import com.colingodsey.scalaminer.utils._
import com.colingodsey.scalaminer.usb.USBManager.{OutputEndpoint, InputEndpoint, Interface}

case object GridSeed extends USBDeviceDriver {
	import USBUtils._

	def hashType: ScalaMiner.HashType = ScalaMiner.Scrypt

	val gsTimeout = 100.millis

	object Constants {
		val MINER_THREADS = 1
		val LATENCY = 4

		val DEFAULT_BAUD = 115200
		val DEFAULT_FREQUENCY = 750
		val DEFAULT_CHIPS = 5
		val DEFAULT_USEFIFO = 0
		val DEFAULT_BTCORE = 16

		val COMMAND_DELAY = 20
		val READ_SIZE = 12
		val MCU_QUEUE_LEN = 0
		val SOFT_QUEUE_LEN = (MCU_QUEUE_LEN + 2)
		val READBUF_SIZE = 8192
		val HASH_SPEED = 0.0851128926.millis
		// in ms
		val F_IN = 25 // input frequency

		val PROXY_PORT = 3350

		val PERIPH_BASE = 0x40000000
		val APB2PERIPH_BASE = (PERIPH_BASE + 0x10000)
		val GPIOA_BASE = (APB2PERIPH_BASE + 0x0800)
		val CRL_OFFSET = 0x00
		val ODR_OFFSET = 0x0c
	}

	val detectBytes = BigInt("55aac000909090900000000001000000",
		16).toByteArray.toIndexedSeq

	case object GSD extends USBIdentity {
		def drv = GridSeed
		def idVendor = 0x0483
		def idProduct = 0x5740
		def iManufacturer = "STMicroelectronics"
		def iProduct = "STM32 Virtual COM Port"
		def config = 1
		def timeout = gsTimeout

		val interfaces = Set(Interface(0, Set(
			//Endpoint(UsbConst.ENDPOINT_TYPE_INTERRUPT, 8, epi(2), 0, false),
			InputEndpoint(UsbConst.ENDPOINT_TYPE_BULK, 64, 1, 0),
			OutputEndpoint(UsbConst.ENDPOINT_TYPE_BULK, 64, 3, 0)
		)))

		override def usbDeviceActorProps(device: UsbDevice, workRef: ActorRef): Props = ???
	}

	case object GSD1 extends USBIdentity {
		def drv = GridSeed
		override def name = "GSD"
		def idVendor = 0x10c4
		def idProduct = 0xea60.toShort
		def iManufacturer = ""
		def iProduct = "CP2102 USB to UART Bridge Controller"
		def config = 1
		def timeout = gsTimeout

		val interfaces = Set(Interface(0, Set(
			InputEndpoint(UsbConst.ENDPOINT_TYPE_BULK, 64, 1, 0),
			OutputEndpoint(UsbConst.ENDPOINT_TYPE_BULK, 64, 1, 0)
		)))

		override def usbDeviceActorProps(device: UsbDevice, workRef: ActorRef): Props = ???
	}

	case object GSD2 extends USBIdentity {
		def drv = GridSeed
		override def name = "GSD"
		def idVendor = FTDI.vendor
		def idProduct = 0x6010
		def iManufacturer = ""
		def iProduct = "Dual RS232-HS"
		def config = 1
		def timeout = gsTimeout

		val interfaces = Set(Interface(0, Set(
			InputEndpoint(UsbConst.ENDPOINT_TYPE_BULK, 512, 1, 0),
			OutputEndpoint(UsbConst.ENDPOINT_TYPE_BULK, 512, 2, 0)
		)))

		override def usbDeviceActorProps(device: UsbDevice, workRef: ActorRef): Props = ???
	}

	case object GSD3 extends USBIdentity {
		def drv = GridSeed
		override def name = "GSD"
		def idVendor = 0x067b
		def idProduct = 0x2303
		def iManufacturer = ""
		def iProduct = "USB-Serial Controller"
		def config = 1
		def timeout = gsTimeout

		val interfaces = Set(Interface(0, Set(
			InputEndpoint(UsbConst.ENDPOINT_TYPE_BULK, 64, 3, 0),
			OutputEndpoint(UsbConst.ENDPOINT_TYPE_BULK, 64, 2, 0)
		)))

		def usbDeviceActorProps(device: UsbDevice, workRef: ActorRef): Props =
			Props(classOf[GSD3Device], device)
	}

	val identities: Set[USBIdentity] = Set(GSD, GSD1, GSD2, GSD3)

	val binFrequency = Seq(
		"55aaef0005006083",
		"55aaef000500038e",
		"55aaef0005000187",
		"55aaef000500438e",
		"55aaef0005008083",
		"55aaef000500838e",
		"55aaef0005004187",
		"55aaef000500c38e",

		"55aaef000500a083",
		"55aaef000500038f",
		"55aaef0005008187",
		"55aaef000500438f",
		"55aaef000500c083",
		"55aaef000500838f",
		"55aaef000500c187",
		"55aaef000500c38f",

		"55aaef000500e083",
		"55aaef0005000188",
		"55aaef0005000084",
		"55aaef0005004188",
		"55aaef0005002084",
		"55aaef0005008188",
		"55aaef0005004084",
		"55aaef000500c188",

		"55aaef0005006084",
		"55aaef0005000189",
		"55aaef0005008084",
		"55aaef0005004189",
		"55aaef000500a084",
		"55aaef0005008189",
		"55aaef000500c084",
		"55aaef000500c189",

		"55aaef000500e084",
		"55aaef000500018a",
		"55aaef0005000085",
		"55aaef000500418a",
		"55aaef0005002085",
		"55aaef000500818a",
		"55aaef0005004085",
		"55aaef000500c18a",

		"55aaef0005006085",
		"55aaef000500018b",
		"55aaef0005008085",
		"55aaef000500418b",
		"55aaef000500a085",
		"55aaef000500818b",
		"55aaef000500c085",
		"55aaef000500c18b",

		"55aaef000500e085",
		"55aaef000500018c",
		"55aaef0005000086",
		"55aaef000500418c",
		"55aaef0005002086",
		"55aaef000500818c",
		"55aaef0005004086",
		"55aaef000500c18c",

		"55aaef0005006086",
		"55aaef000500018d",
		"55aaef0005008086",
		"55aaef000500418d",
		"55aaef000500a086",
		"55aaef000500818d",
		"55aaef000500c086",
		"55aaef000500c18d",

		"55aaef000500e086"
	).map(_.fromHex)
}

class GSD3Device(val device: UsbDevice) extends PL2303Device {
	def receive = ??? //initReceive
	def normal = ???
	def identity = GridSeed.GSD3

	val defaultReadSize: Int = 2000

	override def preStart = {
		super.preStart()

		initUART()
	}


}

trait GridSeedFTDI extends USBDeviceActor {
	import FTDI._
	import GridSeed._
	import Constants._

	override def isFTDI = true

	override def commandDelay = 2.millis
	override def defaultTimeout = 100.seconds

	def jobTimeout = 5.minutes
	val defaultReadSize: Int = 0x2000 // ?

	val pollDelay = 10.millis
	val maxWorkQueue = 15
}

trait PL2303Device extends USBDeviceActor {
	import PL2303._

	def identity: USBIdentity
	def normal: Actor.Receive

	lazy val interfaceDef = identity.interfaces.toSeq.sortBy(_.interface).head

	//set data control
	val ctrlIrp = device.createUsbControlIrp(
		CTRL_OUT,
		REQUEST_CTRL,
		VALUE_CTRL,
		interfaceDef.interface
	)
	ctrlIrp.setData(Array.empty)

	val lineCtrlIrp = device.createUsbControlIrp(
		CTRL_OUT,
		REQUEST_LINE,
		VALUE_LINE,
		interfaceDef.interface
	)
	lineCtrlIrp.setData(byteArrayFrom { x =>
		x.writeInt(VALUE_LINE0)
		x.writeInt(VALUE_LINE1)
	})

	val vendorIrp = device.createUsbControlIrp(
		VENDOR_OUT,
		REQUEST_VENDOR,
		VALUE_VENDOR,
		interfaceDef.interface
	)
	vendorIrp.setData(Array.empty)

	val initIrps = List(ctrlIrp, lineCtrlIrp, vendorIrp)

	lazy val interface = {
		val configuration = device.getActiveUsbConfiguration
		configuration.getUsbInterface(interfaceDef.interface.toByte)
	}

	lazy val outputEndpoint = {
		val op = interfaceDef.endpoints.filter(_.isOutput).head
	}

	def initUART() = runIrps(initIrps)(_ => context become normal)

}
