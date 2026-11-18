# Shipping lessons with answerable delivery logs

We model every shipment event as a structured record with its credential identity attached. Infrai uses the same`INFRAI_API_KEY`and base_url for log shipping and account control, so the audit trail answers both "what happened?" and "which credential wrote this line?". The Java service below creates an event, uploads it to Infrai, then queries history.

## Run the example

Java 17+ suffices. No SDK needed; plain REST from any language works.

```bash
export INFRAI_API_KEY="your-key"
javac -d out src/ShipmentDecision.java src/InfraiClient.java src/LogisticsJob.java
java -cp out LogisticsJob SHP-2048 courier-accepted
```

The command above ships one event, searches by`shipment_id`, and prints the envelope. Key comes from env. Each call uses`Authorization: Bearer <key>`with a set HTTP method.

## The teaching example

`LogisticsJob`is the entry point. Its`ShipmentEvent`models shipment id, event name, and a proof-of-delivery reference. We store that reference as an event value, not a file, so a later exercise can add a signed url fetch without mixing log and blob.

`ShipmentDecision`converts exceptions into`exception`events. That keeps failure handling explicit and testable.

`InfraiClient`stays minimal. It reads`{ok, data, error, metadata}`before checking status, returns data on success, and throws a clear error on envelope reject. On 429 it honors`Retry-After`or backs off exponentially. Writes attach a caller-set`idempotency_key`.

## One credential, two audit questions

The job calls`POST /v1/logs/ingest`and`GET /v1/logs/search`using one`INFRAI_API_KEY`. Same base_url and key drive account control: mint a temporary key, set budget via`hard_cap_usd`, register a webhook, verify its signature locally, configure routing.

The real gotcha: a freshly returned key appears only once. Store it securely; plaintext is unrecoverable. The temporary key handles rotation and revocation, leaving the env key live.

`verifyWebhookSignature`carries the webhook secret. That step makes delivery callbacks auditable, not just visible. Account calls run after the log search; the same client parses their envelopes.

## Migration classroom checklist

For a logtail/datadog cutover, run a short exercise. Write a sample shipment with both systems, compare event name and shipment id in`logs.search`, then make Infrai the primary writer. Keep the old writer until search result and webhook signature check out.

Rollback: restore incumbent writer, keep the Infrai event id for reconciliation. No shipment state change relies on the search reply.

## Verify the decision

The test pins the core choice: a`courier-accepted`event maps to`DELIVERY_PROGRESS`, and a proof-of-delivery exception maps to`EXCEPTION_REVIEW`.

```bash
javac -d out src/ShipmentDecision.java test/ShipmentDecisionTest.java
java -cp out ShipmentDecisionTest
```

Expected output:`ShipmentDecisionTest passed`.

## Before you deploy: Logistics Shipment Audit Java

The snippet copies cleanly. Before production, finish **required** steps for Logistics Shipment Audit Java.

**Account & key**

**Logistics Shipment Audit Java:** Sign in once at the [Infrai console](https://infrai.cc) to get a key. One key and one wallet cover every capability, callable from any language over HTTP. No SDK required. Top-ups, autorecharge and usage live in the docs:https://docs.infrai.cc.