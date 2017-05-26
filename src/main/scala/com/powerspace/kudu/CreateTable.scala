package com.powerspace.kudu

import com.powerspace.kudu.cli.CreateTableCliParser
import com.powerspace.kudu.converters.{AvroColumnBuilder, ColumnBuilder, SqlColumnBuilder}
import org.apache.kudu.{ColumnSchema, Schema}
import org.apache.kudu.ColumnSchema.CompressionAlgorithm
import org.apache.kudu.client.AsyncKuduClient.AsyncKuduClientBuilder
import org.apache.kudu.client.CreateTableOptions
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.io.Source


case class HashedKey(name: String, buckets: Int = 32)
case class CreateTableConfig(
                   tableName: String = "demo",
                   pkeys: List[HashedKey] = List(HashedKey("id")),
                   avroSchemaPath: Option[String] = None,
                   kuduServers: List[String] = List(),
                   sql: Option[String] = None,
                   compressed: Boolean = true,
                   replica: Int = 3,
                   buckets: Int = 32
                 )

object CreateTable extends App {
  val logger = LoggerFactory.getLogger(getClass)

  CreateTableCliParser.parse(args) match {
    case Some(config) => createTable(config)
    case None =>
  }

  def createTable(config: CreateTableConfig): Unit = {
    logger.info(config.toString)

    val columns = buildKuduColumns(converter(config), config.pkeys, config.compressed)
    val options = buildKuduTableOptions(config)

    val newTableName = config.tableName

    logger.info(s"Creating table $newTableName...")
    val client = new AsyncKuduClientBuilder(config.kuduServers.asJava).build()
    client.createTable(newTableName, new Schema(columns.asJava), options).join()
    logger.info(s"Table $newTableName created!")
  }

  def buildKuduTableOptions(config: CreateTableConfig): CreateTableOptions = {
     config.pkeys.foldLeft(new CreateTableOptions().setNumReplicas(config.replica))((acc, key) =>
      acc.addHashPartitions(List(key.name).asJava, key.buckets)
    )
  }

  def buildKuduColumns(converter: ColumnBuilder, pkeys: List[HashedKey], compressed: Boolean): List[ColumnSchema] = {
    // we must order by "key" first for Kudu
    //implicit def orderingByName[A <: ColumnSchema]: Ordering[A] = Ordering.by(!_.isKey)
    val compression = if (compressed) CompressionAlgorithm.LZ4 else CompressionAlgorithm.NO_COMPRESSION
    val pkeyNames = pkeys.map(_.name)

    converter.kuduColumns(compression, pkeyNames)
  }

  private def converter(config: CreateTableConfig): ColumnBuilder = {
    (config.avroSchemaPath.map(file => AvroColumnBuilder(Source.fromFile(file).mkString))
     orElse config.sql.map(SqlColumnBuilder(_, config.pkeys.map(_.name))))
      .getOrElse(???)
  }
}
