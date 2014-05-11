
/*
 * ScalaMiner
 * ----------
 * https://github.com/colinrgodsey/scalaminer
 *
 * Copyright 2014 Colin R Godsey <colingodsey.com>
 * Copyright 2011-2014 Con Kolivas
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.  See COPYING for more details.
 */

package com.colingodsey.scalaminer.api

import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import akka.io.{Tcp, IO}
import java.net.{InetAddress, InetSocketAddress}
import akka.util.ByteString
import com.colingodsey.ScalaMinerVersion
import com.colingodsey.scalaminer.metrics.{Counter, MinerMetrics}
import com.colingodsey.scalaminer.metrics.MinerMetrics.{Metric, Identity}
import com.colingodsey.scalaminer.ScalaMiner
import com.colingodsey.scalaminer.metrics.MinerMetrics._

object CGMinerAPI {
	sealed trait Command

	case class ReceiveCommand(cmd: String) extends Command

	sealed trait CGMinerCommand extends Command

	case object Version extends CGMinerCommand
	case object Summary extends CGMinerCommand
	case object DevDetails extends CGMinerCommand

	val cmdSet = Set(Version, Summary, DevDetails)

	val cmdMap = cmdSet.map(x => x.toString.toLowerCase -> x).toMap
}

class CGMinerAPI(metricsRef: ActorRef, hashType: ScalaMiner.HashType)
		extends Actor with Stash with ActorLogging {
	import CGMinerAPI._

	private implicit def ec = context.system.dispatcher

	implicit val system = context.system

	def port = 4028
	def now = System.currentTimeMillis() / 1000
	def localAddr = new InetSocketAddress(port)

	def snapshotInterval = 3.seconds

	val started = now

	var currentConnections = Set.empty[ActorRef]
	var metricsSnapshot: Map[Identity,
			Map[Metric, Counter.Snapshot]] = Map.empty
	var snapshotTotal: Map[Metric, Counter.Snapshot] = Map.empty

	def metricsSet = metricsSnapshot.flatMap(_._2.keySet).toSet
	def snapshotSumOf(metric: Metric) = (for {
		(ident, metrics) <- metricsSnapshot.toSeq
		(aMetric, snapshot) <- metrics
		if aMetric == metric
	} yield snapshot).reduceLeft(_ + _)
	def calcTotal = metricsSet.toSeq.map(metric => metric -> snapshotSumOf(metric)).toMap

	def sumOf(metric: Metric) = snapshotTotal.get(metric) match {
		case None => 0.0
		case Some(x) => x.sum
	}

	def rateOf(metric: Metric) = snapshotTotal.get(metric) match {
		case None => 0.0
		case Some(x) => x.rate
	}

	def version = ScalaMinerVersion.str

	def descriptionTag = "ScalaMiner " + version

	def formatSuccess(code: Int, msg: String) = {
		List(
			"STATUS" -> "S",
			"When" -> now.toString,
			"Code" -> code,
			"Msg" -> msg,
			"Description" -> descriptionTag
		).map(x => x._1 + "=" + x._2).mkString(",")
	}

	def respondWith(str: String) =
		sender ! Tcp.Write(ByteString(str + "|"))

	def receive = {
		case MinerMetrics.SnapshotResponse(snapshot) =>
			metricsSnapshot = snapshot.filter(_._1.hashType == hashType)
			snapshotTotal = calcTotal
		case Tcp.Connected(remote, _) =>
			log.info("New connection!")
			val connection = sender
			connection ! Tcp.Register(self)
			currentConnections += connection
			context watch connection
		//TODO: this should probably realllly buffer..... long commands may die
		case Tcp.Received(dat) =>
			val cmd = new String(dat.toArray, "ASCII").trim

			self.tell(ReceiveCommand(cmd.substring(0, cmd.length - 1)), sender)
		case ReceiveCommand(cmd) =>
			log.info("Received command " + cmd)

			val msg = cmd.toLowerCase.trim match {
				case x if cmdMap contains x => cmdMap get x
				case x =>
					log.warning("Unknown command " + x)

					val err = s"STATUS=E,When=$now,Code=14,Msg=Invalid command," +
							s"Description=$descriptionTag|"

					sender ! Tcp.Write(ByteString(err))

					None
			}

			msg.foreach(self.tell(_, sender))
		case Tcp.Bound(_) =>
			log.info("Listening on " + localAddr)
		case Tcp.CommandFailed(_: Tcp.Bind) =>
			sys.error("Failed to bind!")
		case Tcp.CommandFailed(cmd) =>
			log.warning("TCP command failed " + cmd)
		case x: Tcp.Message =>
			log.warning("Unhandled tcp command " + x)

		case Terminated(ref) if currentConnections(ref) =>
			currentConnections -= ref
			log.info("Connection closed")

		case Version =>
			respondWith(formatSuccess(22, "ScalaMiner versions"))
			respondWith(s"VERSION,CGMiner=$version,API=1.26")

		case Summary =>
			/*
			STATUS=S,When=1399685471,Code=11,Msg=Summary,Description=cgminer 3.3.1|SUMMARY,Elapsed=268191,MHS av=7475.41,
			Found Blocks=0,Getworks=6862,Accepted=28792,Rejected=83,Hardware Errors=8052,Utility=6.44,Discarded=13720,
			Stale=0,Get Failures=0,Local Work=502148,Remote Failures=0,Network Blocks=512,Total MH=2004834899.1967,Work Utility=104.57,
			Difficulty Accepted=460672.00000000,Difficulty Rejected=1328.00000000,Difficulty Stale=0.00000000,Best Share=1070416
			 */
			respondWith(formatSuccess(11, "Summary"))
			respondWith(s"SUMMARY,Elapsed=${now - started}},MHS av=${rateOf(Hashes) / 1000000}}," +
					s"Found Blocks=0,Getworks=${sumOf(WorkStarted)}},Accepted=${sumOf(NonceAccepted)}," +
					s"Rejected=${sumOf(NonceStale) + sumOf(NonceStratumLow) + sumOf(NonceFail)}," +
					s"Hardware Errors=0,Utility=0,Discarded=${sumOf(NonceShort)},Stale=${sumOf(NonceStale)}," +
					s"Get Failures=0,Local Work=0," +
					s"Remote Failures=0,Network Blocks=512,Total MH=${sumOf(Hashes) / 1000000}," +
					s"Work Utility=0," +
					s"Difficulty Accepted=${sumOf(NonceAccepted)},Difficulty Rejected=${sumOf(NonceShort)}," +
					s"Difficulty Stale=0.00000000,Best Share=0")

		case DevDetails =>
			/*
			STATUS=S,When=1399691062,Code=69,Msg=Device Details,Description=cgminer 3.3.1|
			DEVDETAILS=0,Name=BAJ,ID=0,Driver=BitForceSC,Kernel=,Model=,Device Path=3:5|
			 */
			respondWith(formatSuccess(11, "Device Details"))

			//TODO: replace with a deviceId -> #id thing
			var i = 0
			metricsSnapshot foreach {
				case (Identity(typ, id, _), metrics) =>
					val str = s"DEVDETAILS=$i,Name=$typ,ID=$i,Driver=${typ.drv}}," +
							s"Kernel=,Model=,Device Path=$id"

					respondWith(str)

					i += 1
			}
	}

	override def preStart() {
		super.preStart()

		IO(Tcp) ! Tcp.Bind(self, localAddr)

		context.system.scheduler.schedule(3.seconds, snapshotInterval,
			metricsRef, MinerMetrics.Snapshot)
	}

	override def postStop() {
		super.postStop()

		context stop self
	}
}
