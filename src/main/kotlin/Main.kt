package flink

import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import java.util.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory


fun main() {
    // Create a logger instance
    val logger = LoggerFactory.getLogger("KafkaFlinkApp")
    val env = StreamExecutionEnvironment.getExecutionEnvironment()

    val kafkaSource = KafkaSource.builder<String>()
        .setBootstrapServers("kafka:9092")
        .setTopics("latency")
        .setGroupId("flink-group")
        .setStartingOffsets(OffsetsInitializer.earliest())
        .setValueOnlyDeserializer(SimpleStringSchema())
        .build()

    val latencyStream = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source")
        .map { json ->
//            logger.info("Received JSON: $json")
            val message = Json.decodeFromString<KafkaMessage>(json)
            Json.decodeFromString<KafkaMessage>(json)
//            logger.info("Parsed KafkaMessage: $message")
            message
        }
        .returns(TypeInformation.of(object : TypeHint<KafkaMessage>() {}))

//    latencyStream.print()

//    val kafkaSource = KafkaSource.builder<KafkaMessage>()
//        .setBootstrapServers("kafka:9092")
//        .setTopics("latency")
//        .setStartingOffsets(OffsetsInitializer.earliest())
//        .setValueOnlyDeserializer(KafkaMessageDeserializer())
//        .build()

//    val stream = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source")
//        .map { value ->
//            try {
//                val jsonNode: JsonNode = ObjectMapper().readTree(value)
//                val timestamp = jsonNode.get("timestamp").asLong()
//                val latency = jsonNode.get("latency").asDouble()
//                KafkaMessage(timestamp, latency)
//            } catch (e: Exception) {
//                println("Error parsing JSON: ${e.message}")
//                KafkaMessage(0, 0.0)  // Handle parsing error gracefully
//            }
//        }
//        .returns(TypeInformation.of(object : TypeHint<KafkaMessage>() {}))

//    stream.print()

    val oneMinuteWindows = latencyStream
        .keyBy({ it.timestamp / 60000 }, TypeInformation.of(object : TypeHint<Long>() {}))
        .window(TumblingProcessingTimeWindows.of(Time.minutes(1)))
        .process(LatencyAggregateProcessFunction())
        .returns(TypeInformation.of(object : TypeHint<AggregateResult>() {}))

    val oneHourWindows = latencyStream
        .keyBy({ it.timestamp / 3600000 }, TypeInformation.of(object : TypeHint<Long>() {}))
        .window(TumblingProcessingTimeWindows.of(Time.hours(1)))
        .process(LatencyAggregateProcessFunction())
        .returns(TypeInformation.of(object : TypeHint<AggregateResult>() {}))
//
    oneMinuteWindows.print()
    oneHourWindows.print()
    env.execute("Flink Kafka Consumer")

//    env.execute("Flink Latency Aggregation")
}
@Serializable
data class KafkaMessage(val timestamp: Long, val latency: Double)
data class AggregateResult(val windowStart: Long, val windowEnd: Long, val p50: Double, val p99: Double)

fun calculateQuantile(latencies: List<Double>, quantile: Double): Double {
    val sorted = latencies.sorted()
    val pos = (sorted.size * quantile).toInt()
    return sorted.getOrElse(pos) { sorted.last() }
}

class LatencyAggregateProcessFunction : ProcessWindowFunction<KafkaMessage, AggregateResult, Long, TimeWindow>() {
    override fun process(key: Long, context: Context, elements: MutableIterable<KafkaMessage>, out: Collector<AggregateResult>) {
        val latencies = elements.map { it.latency }
        val logger = LoggerFactory.getLogger("KafkaFlinkAppAggregation")
        val result = AggregateResult(
            context.window().start,
            context.window().end,
            calculateQuantile(latencies, 0.50),
            calculateQuantile(latencies, 0.99)
        )
        logger.info("Aggregated result for ${context.window().start} to ${context.window().end}: $result")
        out.collect(result)
    }
}
