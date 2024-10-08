import time
import json
import random
from kafka import KafkaProducer

producer = KafkaProducer(
    bootstrap_servers='kafka:9092',
    value_serializer=lambda v: json.dumps(v).encode('utf-8'),
    max_request_size=200000000  # 200 MB
)
topic = 'latency'

while True:
    latency_record = {
        'timestamp': int(time.time()),
        'latency': random.uniform(0, 500)  # Simulated latency in milliseconds
    }
    producer.send(topic, latency_record)
    time.sleep(1)  # Adjust the sleep time as needed
